package passway.example.personal.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import passway.example.personal.mfa.MfaMethod;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserProfileResponse(
    String id,
    String username,
    String email,
    String phoneNumber,
    String provider,
    boolean mfaEnabled,
    List<MfaMethod> mfaMethods
) {}
