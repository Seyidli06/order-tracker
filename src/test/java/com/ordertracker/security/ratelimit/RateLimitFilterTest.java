package com.ordertracker.security.ratelimit;

import com.ordertracker.security.handler.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RateLimitFilterTest {

    private SecurityErrorResponseWriter errorResponseWriter;
    private FilterChain filterChain;
    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() throws Exception {
        errorResponseWriter =
                mock(SecurityErrorResponseWriter.class);

        filterChain =
                mock(FilterChain.class);

        rateLimitFilter =
                new RateLimitFilter(
                        errorResponseWriter
                );

        doAnswer(invocation -> {
            HttpServletResponse response =
                    invocation.getArgument(1);

            HttpStatus status =
                    invocation.getArgument(2);

            response.setStatus(
                    status.value()
            );

            return null;
        }).when(
                errorResponseWriter
        ).write(
                any(HttpServletRequest.class),
                any(HttpServletResponse.class),
                any(HttpStatus.class),
                any(String.class)
        );
    }

    @Test
    void shouldAllowFiveLoginRequestsAndRejectSixth()
            throws Exception {

        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request =
                    createRequest(
                            "POST",
                            "/api/auth/login",
                            "10.0.0.1"
                    );

            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            rateLimitFilter.doFilter(
                    request,
                    response,
                    filterChain
            );

            assertEquals(
                    HttpStatus.OK.value(),
                    response.getStatus()
            );

            assertEquals(
                    "5",
                    response.getHeader(
                            "X-RateLimit-Limit"
                    )
            );
        }

        MockHttpServletRequest blockedRequest =
                createRequest(
                        "POST",
                        "/api/auth/login",
                        "10.0.0.1"
                );

        MockHttpServletResponse blockedResponse =
                new MockHttpServletResponse();

        rateLimitFilter.doFilter(
                blockedRequest,
                blockedResponse,
                filterChain
        );

        assertEquals(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                blockedResponse.getStatus()
        );

        assertEquals(
                "5",
                blockedResponse.getHeader(
                        "X-RateLimit-Limit"
                )
        );

        assertEquals(
                "0",
                blockedResponse.getHeader(
                        "X-RateLimit-Remaining"
                )
        );

        assertNotNull(
                blockedResponse.getHeader(
                        "Retry-After"
                )
        );

        verify(
                filterChain,
                times(5)
        ).doFilter(
                any(),
                any()
        );

        verify(
                errorResponseWriter,
                times(1)
        ).write(
                any(HttpServletRequest.class),
                any(HttpServletResponse.class),
                eq(HttpStatus.TOO_MANY_REQUESTS),
                eq(
                        "Too many requests. Please try again later"
                )
        );
    }

    @Test
    void shouldUseSeparateBucketsForDifferentIps()
            throws Exception {

        for (int i = 0; i < 5; i++) {
            rateLimitFilter.doFilter(
                    createRequest(
                            "POST",
                            "/api/auth/login",
                            "10.0.0.1"
                    ),
                    new MockHttpServletResponse(),
                    filterChain
            );
        }

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        rateLimitFilter.doFilter(
                createRequest(
                        "POST",
                        "/api/auth/login",
                        "10.0.0.2"
                ),
                response,
                filterChain
        );

        assertEquals(
                HttpStatus.OK.value(),
                response.getStatus()
        );
    }

    @Test
    void shouldSkipRateLimitForUnconfiguredEndpoint()
            throws Exception {

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        rateLimitFilter.doFilter(
                createRequest(
                        "GET",
                        "/actuator/health",
                        "10.0.0.1"
                ),
                response,
                filterChain
        );

        assertEquals(
                HttpStatus.OK.value(),
                response.getStatus()
        );

        verify(
                errorResponseWriter,
                never()
        ).write(
                any(),
                any(),
                any(),
                any()
        );
    }

    private MockHttpServletRequest createRequest(
            String method,
            String path,
            String remoteAddress
    ) {
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setMethod(method);
        request.setRequestURI(path);
        request.setRemoteAddr(
                remoteAddress
        );

        return request;
    }
}