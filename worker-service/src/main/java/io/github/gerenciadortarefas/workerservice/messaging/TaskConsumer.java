package io.github.gerenciadortarefas.workerservice.messaging;

import io.github.gerenciadortarefas.workerservice.dto.TaskMessage;
import io.github.gerenciadortarefas.workerservice.service.TaskWorkerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskConsumer {

    private final TaskWorkerService taskWorkerService;

    @RabbitListener(queues = "${task.messaging.queue}")
    public void receive(TaskMessage message) {
        log.info("Mensagem recebida da fila. taskId={}, type={}, retryCount={}",
                message.getTaskId(), message.getType(), message.getRetryCount());
        taskWorkerService.processTaskMessage(message);
    }
}

