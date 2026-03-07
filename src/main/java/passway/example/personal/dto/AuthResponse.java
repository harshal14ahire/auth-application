package passway.example.personal.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import passway.example.personal.model.MfaMethod;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String token;
    private String tokenType;
    private boolean mfaRequired;
    private String mfaToken;
    private List<MfaMethod> mfaMethods;
    private String message;

    public static AuthResponse success(String token) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .mfaRequired(false)
                .message("Authentication successful")
                .build();
    }

    public static AuthResponse mfaRequired(String mfaToken, List<MfaMethod> methods) {
        return AuthResponse.builder()
                .mfaRequired(true)
                .mfaToken(mfaToken)
                .mfaMethods(methods)
                .message("MFA verification required")
                .build();
    }
}
