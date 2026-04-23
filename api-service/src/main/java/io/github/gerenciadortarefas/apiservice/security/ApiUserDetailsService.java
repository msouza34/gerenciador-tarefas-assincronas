package io.github.gerenciadortarefas.apiservice.security;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApiUserDetailsService implements UserDetailsService {

    private final AuthProperties authProperties;
    private final PasswordEncoder passwordEncoder;

    private UserDetails configuredUser;

    @PostConstruct
    public void initializeUser() {
        this.configuredUser = User.builder()
                .username(authProperties.getUsername())
                .password(passwordEncoder.encode(authProperties.getPassword()))
                .roles("API_USER")
                .build();
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        if (!configuredUser.getUsername().equals(username)) {
            throw new UsernameNotFoundException("Usuario nao encontrado.");
        }
        return configuredUser;
    }
}
