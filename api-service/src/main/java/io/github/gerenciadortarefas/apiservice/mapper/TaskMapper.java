package io.github.gerenciadortarefas.apiservice.mapper;

import io.github.gerenciadortarefas.apiservice.dto.TaskResponse;
import io.github.gerenciadortarefas.apiservice.entity.Task;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TaskMapper {

    public TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .type(task.getType())
                .status(task.getStatus())
                .payload(task.getPayload())
                .result(task.getResult())
                .retryCount(task.getRetryCount())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}

