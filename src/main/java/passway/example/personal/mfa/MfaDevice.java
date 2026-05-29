package passway.example.personal.mfa;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "mfa_devices")
public class MfaDevice {

    @Id
    String id;

    @Indexed
    String userId;

    @Indexed(unique = true)
    String credentialId;

    byte[] publicKey;

    long signCount;

    String deviceName;

    String attestationType;

    @CreatedDate
    Instant createdAt;
}
