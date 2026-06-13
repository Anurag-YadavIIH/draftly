package com.airtribe.draftly.service.auth;

import com.airtribe.draftly.domain.User;
import com.airtribe.draftly.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loads Draftly {@link User} accounts for Spring Security, keyed by email.
 * Every authenticated user gets the same {@code ROLE_USER} authority - the
 * fine-grained authorization (who owns which draft/email) happens in the
 * service layer, not via roles.
 */
@Service
public class DraftlyUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DraftlyUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + email));
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), user.getPasswordHash(), List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
