package passway.example.personal.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import passway.example.personal.mfa.MfaMethod;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
    String token,
    String tokenType,
    boolean mfaRequired,
    String mfaToken,
    List<MfaMethod> mfaMethods,
    String message
) {
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
