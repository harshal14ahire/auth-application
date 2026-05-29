package passway.example.personal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        JwtProperties jwt,
        TotpProperties totp,
        TwilioProperties twilio,
        OtpProperties otp,
        WebauthnProperties webauthn,
        CorsProperties cors
) {
    public record JwtProperties(String secret, long expirationMs, long mfaExpirationMs) {
    }

    public record TotpProperties(String issuer) {
    }

    public record TwilioProperties(String accountSid, String authToken, String phoneNumber) {
    }

    public record OtpProperties(int expirationSeconds) {
    }

    public record WebauthnProperties(String rpId, String rpName, String origin) {
    }

    public record CorsProperties(List<String> allowedOrigins) {
    }
}
