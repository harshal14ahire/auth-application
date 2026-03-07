package passway.example.personal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebAuthnRegistrationRequest {

    private String deviceName;
    private Map<String, Object> attestation;
}
