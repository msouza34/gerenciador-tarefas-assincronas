package io.github.gerenciadortarefas.workerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskMessage implements Serializable {
    private UUID taskId;
    private String type;
    private String payload;
    private Integer retryCount;
}

