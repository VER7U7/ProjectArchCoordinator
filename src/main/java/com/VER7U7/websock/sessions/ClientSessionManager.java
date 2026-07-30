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
public class ClientSessionManager {
    private final Logger LOGGER = LogManager.getLogger(ClientSessionManager.class);

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


    /**
     * Sends a text payload to a specific player via their active WebSocket session.
     * <p>
     * Note: If an {@link IOException} occurs during transmission, the error is logged
     * internally, but no exception is propagated to the caller.
     *
     * @param playerId the ID of the target player
     * @param payload the text message content to be sent
     * @throws SocketException if the player is not connected or their session is closed
     * */
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

    /**
     * Sends an object payload to the provided session in {@code JSON} format.
     * <p>
     * Note: If an {@link Exception} occurs during transmission, the error is logged
     * internally, but no exception is propagated to the caller.
     *
     * @param session the {@link WebSocketSession} assigned to the client
     * @param payload the object to be serialized into {@code JSON} format
     * */
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

    /**
     * Disconnects the client via their WebSocket session by sending a closure reason message.
     * <p>
     * Note: If an {@link Exception} occurs during transmission, the error is logged
     *      internally, but no exception is propagated to the caller.
     *
     * @param session the {@link WebSocketSession} assigned to the client
     * @param message the reason object to be serialized into {@code JSON} format
     * */
    public void kick(WebSocketSession session, StatusResponse message) {
        if (session != null && session.isOpen()) {
            try {
                sendObject(session, new WsMessage<>("LOGOUT", message));
                session.close(CloseStatus.GOING_AWAY);
            }catch (Exception e) {
                LOGGER.error("Error kicking user: {}", e.getMessage());
            }
        }
    }
}
