package passway.example.personal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import passway.example.personal.model.MfaMethod;

@Data
public class MfaSendRequest {

    @NotBlank(message = "MFA token is required")
    private String mfaToken;

    @NotNull(message = "MFA method is required")
    private MfaMethod method;
}
