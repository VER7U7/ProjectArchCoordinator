package com.VER7U7.websock.sessions;

import com.VER7U7.dto.client.StatusResponse;
import com.VER7U7.dto.common.WsMessage;
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
public class ServerSessionManager {
    private final Logger LOGGER = LogManager.getLogger(ClientSessionManager.class);

    private final ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void registerServer(String serverId, WebSocketSession session) {
        sessions.put(serverId, session);
    }

    public WebSocketSession getServerSession(String serverId) {
        return sessions.get(serverId);
    }

    public void removeServer(String serverId) {
        sessions.remove(serverId);
    }

    public boolean validateSession(WebSocketSession session) {
        return sessions.containsValue(session);
    }

    /**
     * Sends an object payload to the provided session in {@code JSON} format.
     * <p>
     * Note: If an {@link Exception} occurs during transmission, the error is logged
     * internally, but no exception is propagated to the caller.
     *
     * @param session the {@link WebSocketSession} assigned to the client
     * @param payload the object to be serialized into {@code JSON} format
     * */
    public <T> void sendObject(WebSocketSession session, WsMessage<T> payload) {
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(payload);
                session.sendMessage(new TextMessage(json));
            } catch (Exception e) {
                LOGGER.error("Error sending JSON: {}",  e.getMessage());
            }
        }
    }

    /**
     * Sends a disconnection message with the specified payload and closes the session.
     *
     * @param session the {@link WebSocketSession} to be disconnected
     * @param message the payload to send before closing
     **/
    public void disconnect(WebSocketSession session, StatusResponse message) {
        if (session != null && session.isOpen()) {
            try {
                sendObject(session, new WsMessage<>("DISCONNECT", message));
                session.close(CloseStatus.GOING_AWAY);
            }catch (Exception e) {
                LOGGER.error("Error disconnecting server: {}", e.getMessage());
            }
        }
    }
}
