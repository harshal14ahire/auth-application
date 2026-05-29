package passway.example.personal.otp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import passway.example.personal.mfa.MfaMethod;
import passway.example.personal.user.User;
import passway.example.personal.otp.SmsService;

@Component
@RequiredArgsConstructor
public class SmsOtpSender implements OtpSender {

    private final SmsService smsService;

    @Override
    public boolean supports(MfaMethod method) {
        return method == MfaMethod.SMS_OTP;
    }

    @Override
    public void send(User user, String code) {
        if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
            throw new RuntimeException("User phone number not configured for SMS OTP");
        }
        smsService.sendOtpSms(user.getPhoneNumber(), code);
    }
}
