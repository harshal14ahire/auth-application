package passway.example.personal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import passway.example.personal.model.User;
import passway.example.personal.repository.UserRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return buildUserDetails(user);
    }

    public UserDetails loadUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return buildUserDetails(user);
    }

    private UserDetails buildUserDetails(User user) {
        Collection<GrantedAuthority> authorities = buildAuthorities(user);

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword() != null ? user.getPassword() : "")
                .authorities(authorities)
                .build();
    }

    private Collection<GrantedAuthority> buildAuthorities(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Add role-based authorities
        user.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));

        // Add FACTOR_PASSWORD authority (Spring Security 7 FactorGrantedAuthority
        // concept)
        authorities.add(new SimpleGrantedAuthority("FACTOR_PASSWORD"));

        return authorities;
    }
}
