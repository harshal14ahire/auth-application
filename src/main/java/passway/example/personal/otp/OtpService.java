package passway.example.personal.otp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import passway.example.personal.config.AppProperties;
import passway.example.personal.mfa.MfaMethod;
import passway.example.personal.otp.OtpToken;
import passway.example.personal.user.User;
import passway.example.personal.otp.OtpTokenRepository;
import passway.example.personal.user.UserRepository;
import passway.example.personal.otp.OtpSender;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private final UserRepository userRepository;
    private final List<OtpSender> otpSenders;
    private final AppProperties appProperties;

    private final SecureRandom secureRandom = new SecureRandom();

    public void generateAndSendOtp(String userId, MfaMethod method) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String otpCode = generateOtpCode();

        OtpToken otpToken = OtpToken.builder()
                .userId(userId)
                .token(otpCode)
                .type(method)
                .expiresAt(Instant.now().plus(appProperties.otp().expirationSeconds(), ChronoUnit.SECONDS))
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
        OtpSender sender = otpSenders.stream()
                .filter(s -> s.supports(method))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported OTP method: " + method));
        sender.send(user, otpCode);
    }

    private String generateOtpCode() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }
}
