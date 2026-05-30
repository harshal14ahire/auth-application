package passway.example.personal.auth;
import passway.example.personal.user.CustomUserDetailsService;
import passway.example.personal.mfa.TotpService;
import passway.example.personal.mfa.WebAuthnService;
import passway.example.personal.mfa.WebAuthnResponse;
import passway.example.personal.otp.OtpService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import passway.example.personal.config.AppProperties;
import passway.example.personal.auth.AuthResponse;
import passway.example.personal.auth.LoginRequest;
import passway.example.personal.auth.MfaVerifyRequest;
import passway.example.personal.auth.RegisterRequest;
import passway.example.personal.auth.AuthProvider;
import passway.example.personal.mfa.MfaMethod;
import passway.example.personal.user.User;
import passway.example.personal.user.UserRepository;
import passway.example.personal.user.PasswordResetToken;
import passway.example.personal.user.PasswordResetTokenRepository;
import passway.example.personal.otp.EmailService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TotpService totpService;
    private final OtpService otpService;
    private final WebAuthnService webAuthnService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final AppProperties appProperties;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .phoneNumber(request.phoneNumber())
                .provider(AuthProvider.LOCAL)
                .build();

        userRepository.save(user);
        log.info("User registered: {}", user.getUsername());

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtService.generateToken(userDetails);

        return AuthResponse.success(token);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if MFA is enabled
        if (user.isMfaEnabled() && !user.getMfaMethods().isEmpty()) {
            String mfaToken = jwtService.generateMfaPendingToken(user.getUsername());
            log.info("MFA required for user: {}", user.getUsername());
            return AuthResponse.mfaRequired(mfaToken, user.getMfaMethods());
        }

        // No MFA — issue full token
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        return AuthResponse.success(token);
    }

    public AuthResponse verifyMfa(MfaVerifyRequest request) {
        if (!jwtService.isMfaPendingToken(request.mfaToken())) {
            throw new RuntimeException("Invalid MFA token");
        }

        String username = jwtService.extractUsername(request.mfaToken());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean verified = verifyMfaCode(user, request.code(), request.method());

        if (!verified) {
            throw new RuntimeException("Invalid MFA verification code");
        }

        // MFA verified — issue full token with MFA factor authorities
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        user.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
        authorities.add(new SimpleGrantedAuthority("FACTOR_PASSWORD"));
        authorities.add(new SimpleGrantedAuthority("FACTOR_MFA"));

        // Add specific factor authority based on method
        switch (request.method()) {
            case TOTP -> authorities.add(new SimpleGrantedAuthority("FACTOR_TOTP"));
            case EMAIL_OTP, SMS_OTP -> authorities.add(new SimpleGrantedAuthority("FACTOR_OTT"));
            case WEBAUTHN -> authorities.add(new SimpleGrantedAuthority("FACTOR_WEBAUTHN"));
        }

        String token = jwtService.generateTokenAfterMfa(username, authorities);
        log.info("MFA verified for user {} via {}", username, request.method());

        return AuthResponse.success(token);
    }

    public void sendLoginOtp(String mfaToken, MfaMethod method) {
        if (!jwtService.isMfaPendingToken(mfaToken)) {
            throw new RuntimeException("Invalid MFA token");
        }

        String username = jwtService.extractUsername(mfaToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (method == MfaMethod.EMAIL_OTP || method == MfaMethod.SMS_OTP) {
            otpService.generateAndSendOtp(user.getId(), method);
        } else {
            throw new RuntimeException("Method does not support OTP");
        }
    }

    public WebAuthnResponse getWebAuthnLoginOptions(String mfaToken) {
        if (!jwtService.isMfaPendingToken(mfaToken)) {
            throw new RuntimeException("Invalid MFA token");
        }
        String username = jwtService.extractUsername(mfaToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return webAuthnService.generateAuthenticationOptions(user.getId());
    }

    public String handleOAuth2Login(OAuth2User oAuth2User, String registrationId) {
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
        String frontendUrl = appProperties.cors().allowedOrigins().getFirst();
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

        return redirectUrl;
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

    private boolean verifyMfaCode(User user, String code, MfaMethod method) {
        return switch (method) {
            case TOTP -> {
                if (user.getTotpSecret() == null) {
                    throw new RuntimeException("TOTP not set up for this user");
                }
                yield totpService.verifyCode(user.getTotpSecret(), code);
            }
            case EMAIL_OTP, SMS_OTP -> otpService.verifyOtp(user.getId(), code, method);
            case WEBAUTHN -> webAuthnService.verifyAuthentication(user.getId(), code);
        };
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.email());
        if (userOpt.isEmpty()) {
            log.warn("Password reset requested for non-existent email: {}", request.email());
            return;
        }

        User user = userOpt.get();
        if (user.getProvider() != AuthProvider.LOCAL) {
            log.warn("Password reset requested for non-local user: {}", request.email());
            throw new RuntimeException("Password reset is only supported for local accounts");
        }

        tokenRepository.deleteByEmail(request.email());

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .email(request.email())
                .expiryDate(Instant.now().plus(15, ChronoUnit.MINUTES))
                .build();

        tokenRepository.save(resetToken);

        String resetLink = appProperties.webauthn().origin() + "/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(request.email(), resetLink);
        log.info("Password reset token generated and email sent for: {}", request.email());
    }

    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = tokenRepository.findByToken(request.token())
                .orElseThrow(() -> new RuntimeException("Invalid or expired password reset token"));

        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            tokenRepository.delete(resetToken);
            throw new RuntimeException("Password reset token has expired");
        }

        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        tokenRepository.delete(resetToken);
        log.info("Password successfully reset for user: {}", user.getUsername());
    }
}
