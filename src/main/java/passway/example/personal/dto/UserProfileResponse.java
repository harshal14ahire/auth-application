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
public class UserProfileResponse {

    private String id;
    private String username;
    private String email;
    private String phoneNumber;
    private String provider;
    private boolean mfaEnabled;
    private List<MfaMethod> mfaMethods;
}
