package passway.example.personal.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import passway.example.personal.model.AuthProvider;
import passway.example.personal.model.User;
import passway.example.personal.repository.UserRepository;
import passway.example.personal.service.JwtService;

import java.io.IOException;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.cors.allowed-origins}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();

        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());
        String providerId = extractProviderId(oAuth2User, registrationId);
        String email = extractEmail(oAuth2User, registrationId);
        String name = extractName(oAuth2User, registrationId);

        // Find or create user
        Optional<User> existingUser = userRepository.findByProviderAndProviderId(provider, providerId);

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            log.info("OAuth2 user logged in: {} via {}", user.getUsername(), provider);
        } else {
            // Check if email already exists
            Optional<User> emailUser = userRepository.findByEmail(email);
            if (emailUser.isPresent()) {
                user = emailUser.get();
                user.setProvider(provider);
                user.setProviderId(providerId);
                userRepository.save(user);
                log.info("OAuth2 linked existing user: {} via {}", user.getUsername(), provider);
            } else {
                user = User.builder()
                        .username(generateUniqueUsername(name))
                        .email(email)
                        .provider(provider)
                        .providerId(providerId)
                        .build();
                userRepository.save(user);
                log.info("OAuth2 new user created: {} via {}", user.getUsername(), provider);
            }
        }

        // Check MFA
        String redirectUrl;
        if (user.isMfaEnabled() && !user.getMfaMethods().isEmpty()) {
            String mfaToken = jwtService.generateMfaPendingToken(user.getUsername());
            redirectUrl = frontendUrl + "/auth/mfa?token=" + mfaToken
                    + "&methods=" + String.join(",", user.getMfaMethods().stream()
                            .map(Enum::name).toList());
        } else {
            // Generate a user details proxy for token generation
            var userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username(user.getUsername())
                    .password("")
                    .authorities("ROLE_USER", "FACTOR_PASSWORD")
                    .build();
            String token = jwtService.generateToken(userDetails);
            redirectUrl = frontendUrl + "/auth/oauth2/callback?token=" + token;
        }

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String extractProviderId(OAuth2User oAuth2User, String registrationId) {
        return switch (registrationId) {
            case "google" -> oAuth2User.getAttribute("sub");
            case "github" -> String.valueOf(oAuth2User.getAttribute("id"));
            default -> oAuth2User.getName();
        };
    }

    private String extractEmail(OAuth2User oAuth2User, String registrationId) {
        String email = oAuth2User.getAttribute("email");
        if (email == null && "github".equals(registrationId)) {
            email = oAuth2User.getAttribute("login") + "@github.com";
        }
        return email;
    }

    private String extractName(OAuth2User oAuth2User, String registrationId) {
        String name = oAuth2User.getAttribute("name");
        if (name == null) {
            name = oAuth2User.getAttribute("login"); // GitHub fallback
        }
        return name != null ? name : "user";
    }

    private String generateUniqueUsername(String baseName) {
        String username = baseName.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (username.isBlank())
            username = "user";

        String candidate = username;
        int counter = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = username + counter++;
        }
        return candidate;
    }
}
