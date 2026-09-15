package cl.duoc.bankxyz.migracion.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BffTokenInterceptor implements HandlerInterceptor {

    private final BffSecurityProperties properties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        BffChannel channel = BffChannel.fromPath(request.getRequestURI()).orElse(null);

        if (channel == null || !properties.isEnabled()) {
            return true;
        }

        String receivedToken = request.getHeader(properties.getHeaderName());
        if (!StringUtils.hasText(receivedToken)) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, channel,
                    "Token BFF requerido en el header " + properties.getHeaderName());
            return false;
        }

        String expectedToken = properties.tokenFor(channel);
        if (!sameToken(receivedToken, expectedToken)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, channel,
                    "Token no autorizado para el canal " + channel.getRole());
            return false;
        }

        request.setAttribute("BFF_CHANNEL", channel.getRole());
        response.setHeader("X-BFF-Channel", channel.getRole());
        return true;
    }

    private boolean sameToken(String receivedToken, String expectedToken) {
        byte[] received = receivedToken.getBytes(StandardCharsets.UTF_8);
        byte[] expected = expectedToken.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(received, expected);
    }

    private void writeError(HttpServletResponse response, int status, BffChannel channel, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"timestamp":"%s","status":%d,"canal":"%s","error":"%s"}
                """.formatted(LocalDateTime.now(), status, channel.getRole(), message));
    }
}
