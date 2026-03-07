package passway.example.personal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import passway.example.personal.dto.AuthResponse;
import passway.example.personal.dto.LoginRequest;
import passway.example.personal.dto.MfaVerifyRequest;
import passway.example.personal.dto.RegisterRequest;
import passway.example.personal.model.AuthProvider;
import passway.example.personal.model.MfaMethod;
import passway.example.personal.model.User;
import passway.example.personal.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TotpService totpService;
    private final OtpService otpService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
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
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByUsername(request.getUsername())
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
        if (!jwtService.isMfaPendingToken(request.getMfaToken())) {
            throw new RuntimeException("Invalid MFA token");
        }

        String username = jwtService.extractUsername(request.getMfaToken());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean verified = verifyMfaCode(user, request.getCode(), request.getMethod());

        if (!verified) {
            throw new RuntimeException("Invalid MFA verification code");
        }

        // MFA verified — issue full token with MFA factor authorities
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        user.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
        authorities.add(new SimpleGrantedAuthority("FACTOR_PASSWORD"));
        authorities.add(new SimpleGrantedAuthority("FACTOR_MFA"));

        // Add specific factor authority based on method
        switch (request.getMethod()) {
            case TOTP -> authorities.add(new SimpleGrantedAuthority("FACTOR_TOTP"));
            case EMAIL_OTP, SMS_OTP -> authorities.add(new SimpleGrantedAuthority("FACTOR_OTT"));
            case WEBAUTHN -> authorities.add(new SimpleGrantedAuthority("FACTOR_WEBAUTHN"));
        }

        String token = jwtService.generateTokenAfterMfa(username, authorities);
        log.info("MFA verified for user {} via {}", username, request.getMethod());

        return AuthResponse.success(token);
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
            case WEBAUTHN -> throw new RuntimeException("WebAuthn verification uses a different flow");
        };
    }
}
