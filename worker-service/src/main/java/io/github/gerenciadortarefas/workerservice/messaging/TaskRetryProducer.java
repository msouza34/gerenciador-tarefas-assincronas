package io.github.gerenciadortarefas.workerservice.messaging;

import io.github.gerenciadortarefas.workerservice.dto.TaskMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskRetryProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${task.messaging.exchange}")
    private String exchangeName;

    @Value("${task.messaging.routing-key}")
    private String routingKey;

    public void sendRetry(TaskMessage message) {
        rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
        log.info("Tarefa reenviada para fila. taskId={}, retryCount={}", message.getTaskId(), message.getRetryCount());
    }
}

