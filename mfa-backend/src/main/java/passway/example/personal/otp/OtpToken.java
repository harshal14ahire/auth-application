package passway.example.personal.otp;
import passway.example.personal.mfa.MfaMethod;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "otp_tokens")
public class OtpToken {

    @Id
    String id;

    @Indexed
    String userId;

    String token;

    MfaMethod type; // EMAIL_OTP or SMS_OTP

    @Indexed(expireAfter = "0s")
    Instant expiresAt;

    @Builder.Default
    boolean used = false;
}
