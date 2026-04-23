package io.github.gerenciadortarefas.apiservice.ratelimit;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "security.rate-limit.create-task")
public class CreateTaskRateLimitProperties {

    @Min(1)
    private int capacity = 20;

    @Min(1)
    private long windowSeconds = 60;
}
