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

import jakarta.servlet.http.HttpServletRequest;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
public class LoginController {

    public static final String AUTHENTICATED = LoginController.class.getName() + ".authenticated";
    private final byte[] expectedPassword;

    public LoginController(@Value("${application.shell.password:}") String password) {
        expectedPassword = password.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping(value = "/login", consumes = "application/json")
    public ResponseEntity<Void> login(@RequestBody LoginRequest credentials, HttpServletRequest request) {
        if (CorsUtils.isCorsRequest(request)) {
            return response(HttpStatus.FORBIDDEN);
        }
        if (expectedPassword.length == 0 || credentials.password() == null || !MessageDigest.isEqual(expectedPassword, credentials.password().getBytes(StandardCharsets.UTF_8))) {
            val session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            return response(HttpStatus.UNAUTHORIZED);
        }
        val session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute(AUTHENTICATED, true);
        return response(HttpStatus.NO_CONTENT);
    }

    private ResponseEntity<Void> response(HttpStatus status) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).build();
    }

    public record LoginRequest(String password) { }

}
