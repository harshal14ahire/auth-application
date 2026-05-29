package passway.example.personal.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import passway.example.personal.mfa.MfaMethod;

public record MfaVerifyRequest(
    @NotBlank(message = "MFA token is required") String mfaToken,
    @NotBlank(message = "Verification code is required") String code,
    @NotNull(message = "MFA method is required") MfaMethod method
) {}
