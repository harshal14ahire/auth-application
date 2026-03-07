package passway.example.personal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    private String password;

    private String phoneNumber;

    @Builder.Default
    private AuthProvider provider = AuthProvider.LOCAL;

    private String providerId;

    @Builder.Default
    private Set<String> roles = new HashSet<>(Set.of("ROLE_USER"));

    @Builder.Default
    private boolean mfaEnabled = false;

    @Builder.Default
    private List<MfaMethod> mfaMethods = new ArrayList<>();

    private String totpSecret;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
