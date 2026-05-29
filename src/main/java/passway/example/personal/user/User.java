package passway.example.personal.user;
import passway.example.personal.auth.AuthProvider;
import passway.example.personal.mfa.MfaMethod;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "users")
public class User {

    @Id
    String id;

    @Indexed(unique = true)
    String username;

    @Indexed(unique = true)
    String email;

    String password;

    String phoneNumber;

    @Builder.Default
    AuthProvider provider = AuthProvider.LOCAL;

    String providerId;

    @Builder.Default
    Set<String> roles = new HashSet<>(Set.of("ROLE_USER"));

    @Builder.Default
    boolean mfaEnabled = false;

    @Builder.Default
    List<MfaMethod> mfaMethods = new ArrayList<>();

    String totpSecret;

    @CreatedDate
    Instant createdAt;

    @LastModifiedDate
    Instant updatedAt;
}
