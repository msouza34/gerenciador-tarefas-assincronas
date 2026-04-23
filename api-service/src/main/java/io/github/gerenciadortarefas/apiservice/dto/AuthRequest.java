package io.github.gerenciadortarefas.apiservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthRequest {

    @NotBlank(message = "O campo username e obrigatorio.")
    private String username;

    @NotBlank(message = "O campo password e obrigatorio.")
    private String password;
}
