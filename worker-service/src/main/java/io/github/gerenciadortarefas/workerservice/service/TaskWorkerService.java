package io.github.gerenciadortarefas.workerservice.service;

import io.github.gerenciadortarefas.workerservice.dto.TaskMessage;
import io.github.gerenciadortarefas.workerservice.entity.Task;
import io.github.gerenciadortarefas.workerservice.enums.TaskStatus;
import io.github.gerenciadortarefas.workerservice.messaging.TaskRetryProducer;
import io.github.gerenciadortarefas.workerservice.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskWorkerService {

    private final TaskRepository taskRepository;
    private final TaskExecutionService taskExecutionService;
    private final TaskRetryProducer taskRetryProducer;

    @Value("${task.max-retries:3}")
    private int maxRetries;

    @Transactional
    public void processTaskMessage(TaskMessage message) {
        Task task = taskRepository.findById(message.getTaskId()).orElse(null);
        if (task == null) {
            log.warn("Tarefa nÃ£o encontrada para processamento. taskId={}", message.getTaskId());
            return;
        }

        if (task.getStatus() == TaskStatus.COMPLETED) {
            log.info("Tarefa jÃ¡ estava completada e serÃ¡ ignorada. taskId={}", task.getId());
            return;
        }

        task.setStatus(TaskStatus.PROCESSING);
        task.setResult("Processando tarefa...");
        taskRepository.save(task);
        log.info("Tarefa em processamento. taskId={}, type={}", task.getId(), task.getType());

        try {
            String result = taskExecutionService.execute(task);
            task.setStatus(TaskStatus.COMPLETED);
            task.setResult(result);
            taskRepository.save(task);
            log.info("Tarefa processada com sucesso. taskId={}", task.getId());
        } catch (Exception exception) {
            handleFailure(task, exception);
        }
    }

    private void handleFailure(Task task, Exception exception) {
        int nextRetry = (task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1;
        task.setRetryCount(nextRetry);

        if (nextRetry < maxRetries) {
            task.setStatus(TaskStatus.PENDING);
            task.setResult("Falha na tentativa " + nextRetry + ". Tarefa reenfileirada. Motivo: " + exception.getMessage());
            taskRepository.save(task);

            TaskMessage retryMessage = TaskMessage.builder()
                    .taskId(task.getId())
                    .type(task.getType())
                    .payload(task.getPayload())
                    .retryCount(nextRetry)
                    .build();
            taskRetryProducer.sendRetry(retryMessage);

            log.warn("Falha no processamento. Tarefa reenfileirada. taskId={}, retryCount={}", task.getId(), nextRetry, exception);
            return;
        }

        task.setStatus(TaskStatus.FAILED);
        task.setResult("Falha definitiva apÃ³s " + nextRetry + " tentativas. Motivo: " + exception.getMessage());
        taskRepository.save(task);
        log.error("Falha definitiva no processamento. taskId={}, retryCount={}", task.getId(), nextRetry, exception);
    }
}

