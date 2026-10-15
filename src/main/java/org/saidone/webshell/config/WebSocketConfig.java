/*
 * Webshell - Browser-based terminal
 * Copyright (c) 2026 Saidone
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom it is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.saidone.webshell.config;

import lombok.val;
import org.jspecify.annotations.NonNull;
import org.saidone.webshell.controller.LoginController;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler webSocketHandler;

    public WebSocketConfig(WebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/shell")
                .addInterceptors(new SessionHandshakeInterceptor());
    }

    public static class SessionHandshakeInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(
                @NonNull ServerHttpRequest request,
                @NonNull ServerHttpResponse response,
                @NonNull WebSocketHandler wsHandler,
                @NonNull Map<String, Object> attributes) {
            if (request instanceof ServletServerHttpRequest servletRequest) {
                val session = servletRequest.getServletRequest().getSession(false);
                if (session != null && Boolean.TRUE.equals(session.getAttribute(LoginController.AUTHENTICATED))) {
                    return true;
                }
            }
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
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
