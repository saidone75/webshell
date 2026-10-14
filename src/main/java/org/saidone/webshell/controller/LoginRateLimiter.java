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

package org.saidone.webshell.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

@Component
public class LoginRateLimiter {

    private static final int MAX_CLIENTS = 10_000;
    private final Map<String, Window> clients = new HashMap<>();
    private final int maxAttempts;
    private final long windowNanos;
    private final LongSupplier ticker;

    @Autowired
    public LoginRateLimiter(
            @Value("${application.shell.login.max-attempts:5}") int maxAttempts,
            @Value("${application.shell.login.window-seconds:60}") long windowSeconds) {
        this(maxAttempts, windowSeconds, System::nanoTime);
    }

    LoginRateLimiter(int maxAttempts, long windowSeconds, LongSupplier ticker) {
        if (maxAttempts < 1 || windowSeconds < 1) {
            throw new IllegalArgumentException("Login rate limits must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.windowNanos = Duration.ofSeconds(windowSeconds).toNanos();
        this.ticker = ticker;
    }

    // Returns zero when allowed, otherwise the seconds until another attempt is allowed
    public synchronized long retryAfter(String address) {
        long now = ticker.getAsLong();
        clients.values().removeIf(window -> now - window.started >= windowNanos);
        var window = clients.get(address);
        if (window == null) {
            if (clients.size() >= MAX_CLIENTS) {
                return Math.max(1, windowNanos / 1_000_000_000L);
            }
            window = new Window(now);
            clients.put(address, window);
        }
        if (window.attempts >= maxAttempts) {
            long remaining = windowNanos - (now - window.started);
            return 1 + (remaining - 1) / 1_000_000_000L;
        }
        window.attempts++;
        return 0;
    }

    private static class Window {

        private final long started;
        private int attempts;

        private Window(long started) {
            this.started = started;
        }

    }
}
