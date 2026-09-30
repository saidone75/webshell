package org.saidone.webshell.handler;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;
import lombok.val;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ShellWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, PtyProcess> processes = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        val osName = System.getProperty("os.name").toLowerCase();
        boolean isWindows = osName.contains("win");

        val cmd = isWindows
                ? new String[]{"powershell.exe", "-NoExit"}
                : new String[]{"/bin/sh", "-i"};

        val env = new HashMap<>(System.getenv());
        env.put("TERM", "xterm-256color");

        val process = new PtyProcessBuilder()
                .setCommand(cmd)
                .setEnvironment(env)
                .start();

        process.setWinSize(new WinSize(80, 24));

        processes.put(session.getId(), process);

        executorService.submit(() -> {
            try (val in = process.getInputStream()) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1 && session.isOpen()) {
                    val text = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                    session.sendMessage(new TextMessage(text));
                }
            } catch (Exception ignored) {
            }
        });
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, @NonNull TextMessage message) throws Exception {
        val process = processes.get(session.getId());
        if (process != null && process.isAlive()) {
            if (message.getPayload().startsWith("resize:")) {
                val dimensions = message.getPayload().substring("resize:".length()).split(":", 2);
                if (dimensions.length == 2) {
                    process.setWinSize(new WinSize(
                            Integer.parseInt(dimensions[0]),
                            Integer.parseInt(dimensions[1])));
                }
                return;
            }
            val out = process.getOutputStream();
            out.write(message.getPayload().getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) {
        val process = processes.remove(session.getId());
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
    }

}
