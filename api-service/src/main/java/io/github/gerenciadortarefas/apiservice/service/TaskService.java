package io.github.gerenciadortarefas.apiservice.service;

import io.github.gerenciadortarefas.apiservice.dto.TaskCreateRequest;
import io.github.gerenciadortarefas.apiservice.dto.TaskMessage;
import io.github.gerenciadortarefas.apiservice.dto.TaskResponse;
import io.github.gerenciadortarefas.apiservice.entity.Task;
import io.github.gerenciadortarefas.apiservice.enums.TaskStatus;
import io.github.gerenciadortarefas.apiservice.exception.TaskNotFoundException;
import io.github.gerenciadortarefas.apiservice.mapper.TaskMapper;
import io.github.gerenciadortarefas.apiservice.messaging.TaskProducer;
import io.github.gerenciadortarefas.apiservice.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskProducer taskProducer;

    public TaskResponse createTask(TaskCreateRequest request) {
        String normalizedType = request.getType().trim();
        String normalizedPayload = request.getPayload().trim();

        Task task = Task.builder()
                .type(normalizedType)
                .status(TaskStatus.PENDING)
                .payload(normalizedPayload)
                .retryCount(0)
                .build();

        Task savedTask = taskRepository.save(task);
        log.info("Tarefa criada com sucesso. id={}, type={}, status={}",
                savedTask.getId(), savedTask.getType(), savedTask.getStatus());

        TaskMessage message = TaskMessage.builder()
                .taskId(savedTask.getId())
                .type(savedTask.getType())
                .payload(savedTask.getPayload())
                .retryCount(savedTask.getRetryCount())
                .build();

        taskProducer.sendTaskMessage(message);

        return TaskMapper.toResponse(savedTask);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listTasks() {
        return taskRepository.findAll()
                .stream()
                .map(TaskMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Tarefa nÃ£o encontrada para o id: " + taskId));
        return TaskMapper.toResponse(task);
    }
}

