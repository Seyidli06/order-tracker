package com.ordertracker.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordertracker.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String message
    ) throws IOException {

        ApiError apiError = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                Map.of()
        );

        response.setStatus(status.value());
        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );
        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );

        objectMapper.writeValue(
                response.getOutputStream(),
                apiError
        );
    }
}