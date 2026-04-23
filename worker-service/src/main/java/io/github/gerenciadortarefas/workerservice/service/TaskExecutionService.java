package io.github.gerenciadortarefas.workerservice.service;

import io.github.gerenciadortarefas.workerservice.entity.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TaskExecutionService {

    @Value("${task.processing.simulated-delay-ms:2000}")
    private long simulatedDelayMs;

    public String execute(Task task) {
        String normalizedType = normalizeType(task.getType());
        simulateWorkDelay();

        if (task.getPayload() != null && task.getPayload().toUpperCase().contains("FORCE_ERROR")) {
            throw new IllegalStateException("Erro simulado por payload contendo FORCE_ERROR.");
        }

        return switch (normalizedType) {
            case "REPORT" -> processReport(task.getPayload());
            case "EMAIL" -> processEmail(task.getPayload());
            case "DATA_PROCESS" -> processData(task.getPayload());
            default -> processGeneric(task.getType(), task.getPayload());
        };
    }

    private String processReport(String payload) {
        log.info("Processando tarefa do tipo REPORT.");
        return "RelatÃ³rio gerado com sucesso para payload: " + payload;
    }

    private String processEmail(String payload) {
        log.info("Processando tarefa do tipo EMAIL.");
        return "SimulaÃ§Ã£o de envio de e-mail concluÃ­da para payload: " + payload;
    }

    private String processData(String payload) {
        log.info("Processando tarefa do tipo DATA_PROCESS.");
        return "Processamento de dados concluÃ­do para payload: " + payload;
    }

    private String processGeneric(String type, String payload) {
        log.info("Processando tarefa genÃ©rica. type={}", type);
        return "Tarefa do tipo " + type + " processada com sucesso. payload: " + payload;
    }

    private String normalizeType(String type) {
        if (type == null) {
            return "";
        }
        return type.trim().toUpperCase();
    }

    private void simulateWorkDelay() {
        try {
            Thread.sleep(simulatedDelayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread interrompida durante simulaÃ§Ã£o de processamento.", exception);
        }
    }
}

