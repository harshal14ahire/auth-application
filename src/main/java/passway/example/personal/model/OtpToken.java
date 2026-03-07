package passway.example.personal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "otp_tokens")
public class OtpToken {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String token;

    private MfaMethod type; // EMAIL_OTP or SMS_OTP

    @Indexed(expireAfter = "0s")
    private Instant expiresAt;

    @Builder.Default
    private boolean used = false;
}
