package passway.example.personal.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import passway.example.personal.user.UserProfileResponse;
import passway.example.personal.user.User;
import passway.example.personal.user.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserProfileResponse getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .provider(user.getProvider().name())
                .mfaEnabled(user.isMfaEnabled())
                .mfaMethods(user.getMfaMethods())
                .build();
    }
}
