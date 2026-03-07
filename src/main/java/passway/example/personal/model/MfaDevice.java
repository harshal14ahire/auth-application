package passway.example.personal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "mfa_devices")
public class MfaDevice {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed(unique = true)
    private String credentialId;

    private byte[] publicKey;

    private long signCount;

    private String deviceName;

    private String attestationType;

    @CreatedDate
    private Instant createdAt;
}
