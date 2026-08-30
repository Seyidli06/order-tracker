package com.ordertracker.security.webhook;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class CachedBodyHttpServletRequest
        extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(
            HttpServletRequest request
    ) throws IOException {

        super(request);

        this.cachedBody =
                request.getInputStream()
                        .readAllBytes();
    }

    public byte[] getCachedBody() {
        return cachedBody.clone();
    }

    @Override
    public ServletInputStream getInputStream() {

        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(
                        cachedBody
                );

        return new ServletInputStream() {

            @Override
            public int read() {
                return inputStream.read();
            }

            @Override
            public boolean isFinished() {
                return inputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(
                    ReadListener readListener
            ) {
                // Synchronous request processing.
            }
        };
    }

    @Override
    public BufferedReader getReader() {

        String encoding =
                getCharacterEncoding();

        if (encoding == null) {
            encoding =
                    StandardCharsets.UTF_8.name();
        }

        return new BufferedReader(
                new InputStreamReader(
                        getInputStream(),
                        java.nio.charset.Charset.forName(
                                encoding
                        )
                )
        );
    }

    @Override
    public int getContentLength() {
        return cachedBody.length;
    }

    @Override
    public long getContentLengthLong() {
        return cachedBody.length;
    }
}