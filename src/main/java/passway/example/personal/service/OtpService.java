package passway.example.personal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import passway.example.personal.model.MfaMethod;
import passway.example.personal.model.OtpToken;
import passway.example.personal.model.User;
import passway.example.personal.repository.OtpTokenRepository;
import passway.example.personal.repository.UserRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    @Value("${app.otp.expiration-seconds}")
    private int otpExpirationSeconds;

    private final SecureRandom secureRandom = new SecureRandom();

    public void generateAndSendOtp(String userId, MfaMethod method) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String otpCode = generateOtpCode();

        OtpToken otpToken = OtpToken.builder()
                .userId(userId)
                .token(otpCode)
                .type(method)
                .expiresAt(Instant.now().plus(otpExpirationSeconds, ChronoUnit.SECONDS))
                .used(false)
                .build();

        otpTokenRepository.save(otpToken);

        deliverOtp(user, otpCode, method);

        log.info("OTP generated and sent for user {} via {}", userId, method);
    }

    public boolean verifyOtp(String userId, String code, MfaMethod method) {
        return otpTokenRepository
                .findFirstByUserIdAndTypeAndUsedFalseOrderByExpiresAtDesc(userId, method)
                .filter(token -> token.getToken().equals(code))
                .filter(token -> token.getExpiresAt().isAfter(Instant.now()))
                .map(token -> {
                    token.setUsed(true);
                    otpTokenRepository.save(token);
                    return true;
                })
                .orElse(false);
    }

    private void deliverOtp(User user, String otpCode, MfaMethod method) {
        switch (method) {
            case EMAIL_OTP -> {
                if (user.getEmail() == null || user.getEmail().isBlank()) {
                    throw new RuntimeException("User email not configured for OTP delivery");
                }
                emailService.sendOtpEmail(user.getEmail(), otpCode);
            }
            case SMS_OTP -> {
                if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
                    throw new RuntimeException("User phone number not configured for SMS OTP");
                }
                smsService.sendOtpSms(user.getPhoneNumber(), otpCode);
            }
            default -> throw new IllegalArgumentException("Unsupported OTP method: " + method);
        }
    }

    private String generateOtpCode() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }
}
