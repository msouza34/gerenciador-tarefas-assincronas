package io.github.gerenciadortarefas.apiservice.security;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "security.auth")
public class AuthProperties {

    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
