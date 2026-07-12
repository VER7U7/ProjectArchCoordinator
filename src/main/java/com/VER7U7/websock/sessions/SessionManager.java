package com.VER7U7.websock.sessions;

import com.VER7U7.dto.Result;
import com.VER7U7.dto.ResultStatus;
import com.VER7U7.models.PlayerAccount;
import com.VER7U7.utils.TraceUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class SessionManager {
    private final Logger LOGGER = LogManager.getLogger(SessionManager.class);

    private final ConcurrentMap<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void registerPlayer(Long playerId, WebSocketSession session) {
        sessions.put(playerId, session);
    }

    public WebSocketSession getPlayerSession(Long playerId) {
        return sessions.get(playerId);
    }

    public void removePlayer(Long playerId) {
        sessions.remove(playerId);
    }

    public boolean validateSession(WebSocketSession session) {
        return sessions.containsValue(session);
    }

    public void sendToPlayer(Long playerId, String payload) throws SocketException {
        WebSocketSession session = sessions.get(playerId);

        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(payload));
            }catch (IOException e) {
                LOGGER.debug(TraceUtils.printStackTrace(e));
            }
        } else {
            throw new SocketException("Player hasn't been connected");
        }
    }

    public void sendObject(WebSocketSession session, Object payload) {
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(payload);
                session.sendMessage(new TextMessage(json));
            } catch (Exception e) {
                LOGGER.error("Error sending JSON: {}",  e.getMessage());
            }
        }
    }

    public void kick(WebSocketSession session, Object message) {
        if (session != null && session.isOpen()) {
            try {
                sendObject(session, new Result("LOGOUT", message));
                session.close(CloseStatus.GOING_AWAY);
            }catch (Exception e) {
                LOGGER.error("Error kicking user: {}", e.getMessage());
            }
        }
    }
}
