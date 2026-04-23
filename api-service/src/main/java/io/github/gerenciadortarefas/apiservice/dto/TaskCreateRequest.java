package io.github.gerenciadortarefas.apiservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskCreateRequest {

    @NotBlank(message = "O campo type e obrigatorio.")
    @Size(max = 50, message = "O campo type deve ter no maximo 50 caracteres.")
    @Pattern(regexp = "^[A-Za-z0-9_\\- ]+$", message = "O campo type possui caracteres invalidos.")
    private String type;

    @NotBlank(message = "O campo payload e obrigatorio.")
    @Size(max = 5000, message = "O campo payload deve ter no maximo 5000 caracteres.")
    private String payload;
}
