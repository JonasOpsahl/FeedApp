package com.gruppe2.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Configuration
@EnableWebSocket
public class RawWebSocketServer implements WebSocketConfigurer {
    private static final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new SimpleTextHandler(), "/rawws").setAllowedOrigins("*");
    }

    public static void broadcast(String message) {
        for (WebSocketSession session : sessions) {
            try {
                session.sendMessage(new org.springframework.web.socket.TextMessage(message));
            } catch (Exception ignored) { }
        }
    }

    public static void broadcastJson(Object payload) {
        try {
            broadcast(MAPPER.writeValueAsString(payload));
        } catch (Exception ignored) { }
    }

    private static class SimpleTextHandler extends TextWebSocketHandler {
        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            sessions.add(session);
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
            sessions.remove(session);
        }

        @Override
        public void handleTextMessage(WebSocketSession session, org.springframework.web.socket.TextMessage message) throws Exception {
            // Echo for simple diagnostics
            session.sendMessage(message);
        }
    }
}
