package com.VER7U7.websock.sessions;

import com.VER7U7.utils.TraceUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class SessionManager {
    private final Logger LOGGER = LogManager.getLogger(SessionManager.class);

    private final ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void registerPlayer(String playerId, WebSocketSession session) {
        sessions.put(playerId, session);
    }

    public void removePlayer(String playerId) {
        sessions.remove(playerId);
    }

    public void sendToPlayer(String playerId, String payload) {
        WebSocketSession session = sessions.get(playerId);

        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(payload));
            }catch (IOException e) {
                LOGGER.debug(TraceUtils.printStackTrace(e));
            }
        }
    }

    public void sendObject(WebSocketSession session, Object payload) {
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(payload);
                session.sendMessage(new TextMessage(json));
            } catch (Exception e) {
                System.err.println("Ошибка при отправке JSON: " + e.getMessage());
            }
        }
    }
}
