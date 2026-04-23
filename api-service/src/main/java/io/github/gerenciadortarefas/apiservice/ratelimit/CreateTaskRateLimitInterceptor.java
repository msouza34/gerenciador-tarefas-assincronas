package io.github.gerenciadortarefas.apiservice.ratelimit;

import io.github.gerenciadortarefas.apiservice.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
@RequiredArgsConstructor
public class CreateTaskRateLimitInterceptor implements HandlerInterceptor {

    private final CreateTaskRateLimitProperties properties;
    private final ConcurrentHashMap<String, Deque<Long>> requestTimelineByKey = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String clientKey = resolveClientKey(request);
        long now = System.currentTimeMillis();
        long windowStart = now - properties.getWindowSeconds() * 1000L;

        Deque<Long> requests = requestTimelineByKey.computeIfAbsent(clientKey, ignored -> new ConcurrentLinkedDeque<>());

        synchronized (requests) {
            while (!requests.isEmpty() && requests.peekFirst() < windowStart) {
                requests.pollFirst();
            }

            if (requests.size() >= properties.getCapacity()) {
                throw new RateLimitExceededException("Limite de requisicoes atingido para criacao de tarefas.");
            }

            requests.addLast(now);
        }

        return true;
    }

    private String resolveClientKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getName() != null) {
            return "user:" + authentication.getName();
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return "ip:" + forwardedFor.split(",")[0].trim();
        }

        return "ip:" + request.getRemoteAddr();
    }
}
