package org.saidone.webshell.config;

import lombok.val;
import org.saidone.webshell.handler.ShellWebSocketHandler;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ShellWebSocketHandler shellWebSocketHandler;
    private final String shellPassword;

    public WebSocketConfig(
            ShellWebSocketHandler shellWebSocketHandler,
            @Value("${application.shell.password:}") String shellPassword) {
        this.shellWebSocketHandler = shellWebSocketHandler;
        this.shellPassword = shellPassword;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(shellWebSocketHandler, "/shell")
                .addInterceptors(new ShellPasswordHandshakeInterceptor(shellPassword))
                .setAllowedOrigins("*");
    }

    private record ShellPasswordHandshakeInterceptor(byte[] expectedPassword) implements HandshakeInterceptor {

        private ShellPasswordHandshakeInterceptor(String expectedPassword) {
            this(expectedPassword.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public boolean beforeHandshake(
                ServerHttpRequest request,
                @NonNull ServerHttpResponse response,
                @NonNull WebSocketHandler wsHandler,
                @NonNull Map<String, Object> attributes) {
            val suppliedPassword = request.getURI().getQuery();
            var password = (String) null;
            if (suppliedPassword != null) {
                for (val parameter : suppliedPassword.split("&")) {
                    val pair = parameter.split("=", 2);
                    if (pair.length == 2 && pair[0].equals("password")) {
                        password = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                        break;
                    }
                }
            }

            val authenticated = password != null
                    && MessageDigest.isEqual(
                    expectedPassword,
                    password.getBytes(StandardCharsets.UTF_8));
            if (!authenticated) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
            }
            return authenticated;
        }

        @Override
        public void afterHandshake(
                @NonNull ServerHttpRequest request,
                @NonNull ServerHttpResponse response,
                @NonNull WebSocketHandler wsHandler,
                Exception exception) {
        }
    }

}
