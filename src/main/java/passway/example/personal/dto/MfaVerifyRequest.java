package passway.example.personal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import passway.example.personal.model.MfaMethod;

@Data
public class MfaVerifyRequest {

    @NotBlank(message = "MFA token is required")
    private String mfaToken;

    @NotBlank(message = "Verification code is required")
    private String code;

    @NotNull(message = "MFA method is required")
    private MfaMethod method;
}
