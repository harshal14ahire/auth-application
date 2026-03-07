package passway.example.personal.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import passway.example.personal.model.MfaMethod;

@Data
public class OtpRequest {

    @NotNull(message = "OTP method is required (EMAIL_OTP or SMS_OTP)")
    private MfaMethod method;
}
