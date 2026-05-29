package passway.example.personal.otp;

import jakarta.validation.constraints.NotNull;
import passway.example.personal.mfa.MfaMethod;

public record OtpRequest(
    @NotNull(message = "OTP method is required (EMAIL_OTP or SMS_OTP)") MfaMethod method
) {}
