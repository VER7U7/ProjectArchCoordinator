package com.VER7U7.websock.handlers;

import com.VER7U7.dto.client.StatusResponse;
import com.VER7U7.dto.common.WsMessage;
import com.VER7U7.service.ClientAuthService;
import com.VER7U7.websock.handlers.client.GameActionHandler;
import com.VER7U7.websock.sessions.ClientSessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SocketCoordinator extends TextWebSocketHandler {
    private final Logger LOGGER = LogManager.getLogger(SocketCoordinator.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, GameActionHandler> handlers;
    private final ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private final ClientAuthService clientAuthService;

    private final ClientSessionManager sessionManager;

    public SocketCoordinator(
            List<GameActionHandler> handlerList,
            ClientSessionManager sessions,
            ClientAuthService clientAuthService) {
        this.clientAuthService = clientAuthService;
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(GameActionHandler::getAction, Function.identity()));
        this.sessionManager = sessions;
    }


    /**
     * Handles incoming WebSocket text messages from client.
     *
     * @param session the {@link WebSocketSession} assigned to the client
     * @param message the {@link TextMessage} containing the request data in {@code JSON} format
     * @throws Exception if an error occurs during message processing
     * */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception{
        String payload = message.getPayload();

        try {
            JsonNode jsonNode = objectMapper.readTree(payload);

            if (!jsonNode.has("action")) {
                return;
            }

            String action = jsonNode.get("action").asText();

            GameActionHandler handler = handlers.get(action);

            if (handler != null) {
                if (handler.mustBeAuthed()) {
                    if (!jsonNode.has("accessToken")) {
                        sessionManager.sendObject(session, new WsMessage<>("AUTH_RESULT", new StatusResponse("need_auth")));
                        return;
                    }

                    String accessToken = jsonNode.get("accessToken").asText();

                    if (!clientAuthService.validateAccessToken(session, accessToken)) {
                        sessionManager.sendObject(session, new WsMessage<>("AUTH_RESULT", new StatusResponse("need_refresh_token")));
                        return;
                    }
                }

                JsonNode data = jsonNode.has("data") ? jsonNode.get("data") : objectMapper.createObjectNode();
                handler.handle(session, data);
            } else {
                LOGGER.debug("Undefined action: {}", action);
            }
        } finally {
            LOGGER.debug("Message from {}: {}", session.getId(), payload);
        }

    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        LOGGER.debug("Player connected. Session ID: {}", session.getId());

        session.sendMessage(new TextMessage("{\"status\": \"connected\"}"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        if (session.getAttributes().containsKey("playerId")) {
            long playerId = Long.parseLong(session.getAttributes().get("playerId").toString());
            sessionManager.removePlayer(playerId);
        }
        LOGGER.debug("Player disconnected: {}", session.getId());
    }

    private boolean needRefreshToken() {
        return false;
    }

    private boolean isAuthed(WebSocketSession session) {
        return session.getAttributes().get("playerId") != null;
    }
}
