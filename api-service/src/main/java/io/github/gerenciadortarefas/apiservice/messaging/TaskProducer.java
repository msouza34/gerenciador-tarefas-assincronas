package io.github.gerenciadortarefas.apiservice.messaging;

import io.github.gerenciadortarefas.apiservice.dto.TaskMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${task.messaging.exchange}")
    private String exchangeName;

    @Value("${task.messaging.routing-key}")
    private String routingKey;

    public void sendTaskMessage(TaskMessage taskMessage) {
        rabbitTemplate.convertAndSend(exchangeName, routingKey, taskMessage);
        log.info("Mensagem enviada para a fila. taskId={}, type={}", taskMessage.getTaskId(), taskMessage.getType());
    }
}

