package com.ordertracker.security.ratelimit;

import com.ordertracker.security.handler.SecurityErrorResponseWriter;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long LOGIN_LIMIT = 5;
    private static final long REGISTER_LIMIT = 3;
    private static final long WEBHOOK_LIMIT = 60;
    private static final long ORDER_LIMIT = 120;

    private static final Duration REFILL_DURATION =
            Duration.ofMinutes(1);

    private final SecurityErrorResponseWriter
            securityErrorResponseWriter;

    private final Cache<String, Bucket> buckets =
            Caffeine.newBuilder()
                    .maximumSize(100_000)
                    .expireAfterAccess(
                            Duration.ofMinutes(10)
                    )
                    .build();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        RateLimitRule rule =
                resolveRule(request);

        if (rule == null) {
            filterChain.doFilter(
                    request,
                    response
            );
            return;
        }

        String clientKey =
                resolveClientKey(
                        request,
                        rule
                );

        String bucketKey =
                rule.name()
                        + ":"
                        + clientKey;

        Bucket bucket =
                buckets.get(
                        bucketKey,
                        ignored ->
                                createBucket(
                                        rule.capacity()
                                )
                );

        ConsumptionProbe probe =
                bucket.tryConsumeAndReturnRemaining(
                        1
                );

        response.setHeader(
                "X-RateLimit-Limit",
                String.valueOf(
                        rule.capacity()
                )
        );

        response.setHeader(
                "X-RateLimit-Remaining",
                String.valueOf(
                        probe.getRemainingTokens()
                )
        );

        if (probe.isConsumed()) {
            filterChain.doFilter(
                    request,
                    response
            );
            return;
        }

        long retryAfterSeconds =
                Math.max(
                        1,
                        TimeUnit.NANOSECONDS
                                .toSeconds(
                                        probe.getNanosToWaitForRefill()
                                )
                );

        response.setHeader(
                "Retry-After",
                String.valueOf(
                        retryAfterSeconds
                )
        );

        securityErrorResponseWriter.write(
                request,
                response,
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests. Please try again later"
        );
    }

    private RateLimitRule resolveRule(
            HttpServletRequest request
    ) {
        String method =
                request.getMethod();

        String path =
                request.getRequestURI();

        if ("POST".equalsIgnoreCase(method)
                && "/api/auth/login".equals(path)) {

            return new RateLimitRule(
                    "login",
                    LOGIN_LIMIT,
                    RateLimitKey.IP
            );
        }

        if ("POST".equalsIgnoreCase(method)
                && "/api/auth/register".equals(path)) {

            return new RateLimitRule(
                    "register",
                    REGISTER_LIMIT,
                    RateLimitKey.IP
            );
        }

        if ("POST".equalsIgnoreCase(method)
                && path.startsWith(
                "/api/webhooks/"
        )) {

            return new RateLimitRule(
                    "webhook",
                    WEBHOOK_LIMIT,
                    RateLimitKey.IP
            );
        }

        if (path.startsWith(
                "/api/orders"
        )) {

            return new RateLimitRule(
                    "orders",
                    ORDER_LIMIT,
                    RateLimitKey.USER
            );
        }

        return null;
    }

    private String resolveClientKey(
            HttpServletRequest request,
            RateLimitRule rule
    ) {
        if (rule.keyType()
                == RateLimitKey.USER) {

            Authentication authentication =
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication();

            if (authentication != null
                    && authentication.isAuthenticated()
                    && authentication.getName() != null) {

                return "user:"
                        + authentication.getName();
            }
        }

        return "ip:"
                + request.getRemoteAddr();
    }

    private Bucket createBucket(
            long capacity
    ) {
        return Bucket.builder()
                .addLimit(limit ->
                        limit
                                .capacity(
                                        capacity
                                )
                                .refillGreedy(
                                        capacity,
                                        REFILL_DURATION
                                )
                )
                .build();
    }

    private record RateLimitRule(
            String name,
            long capacity,
            RateLimitKey keyType
    ) {
    }

    private enum RateLimitKey {
        IP,
        USER
    }
}