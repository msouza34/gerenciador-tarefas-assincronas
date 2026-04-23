package io.github.gerenciadortarefas.apiservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gerenciadortarefas.apiservice.dto.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         org.springframework.security.core.AuthenticationException authException) throws IOException {
        writeErrorResponse(
                response,
                HttpStatus.UNAUTHORIZED,
                "Nao autenticado. Informe um token JWT valido.",
                request.getRequestURI()
        );
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        writeErrorResponse(
                response,
                HttpStatus.FORBIDDEN,
                "Acesso negado para este recurso.",
                request.getRequestURI()
        );
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message, String path)
            throws IOException {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
