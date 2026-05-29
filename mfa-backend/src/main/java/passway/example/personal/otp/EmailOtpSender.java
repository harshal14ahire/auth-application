package passway.example.personal.otp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import passway.example.personal.mfa.MfaMethod;
import passway.example.personal.user.User;
import passway.example.personal.otp.EmailService;

@Component
@RequiredArgsConstructor
public class EmailOtpSender implements OtpSender {

    private final EmailService emailService;

    @Override
    public boolean supports(MfaMethod method) {
        return method == MfaMethod.EMAIL_OTP;
    }

    @Override
    public void send(User user, String code) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new RuntimeException("User email not configured for OTP delivery");
        }
        emailService.sendOtpEmail(user.getEmail(), code);
    }
}
