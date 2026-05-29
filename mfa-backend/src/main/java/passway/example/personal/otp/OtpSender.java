package passway.example.personal.otp;

import passway.example.personal.mfa.MfaMethod;
import passway.example.personal.user.User;

public interface OtpSender {
    boolean supports(MfaMethod method);
    void send(User user, String code);
}
