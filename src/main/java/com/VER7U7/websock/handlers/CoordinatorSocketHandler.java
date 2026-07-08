package com.VER7U7.websock.handlers;

import com.VER7U7.dto.ResultStatus;
import com.VER7U7.websock.sessions.SessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CoordinatorSocketHandler extends TextWebSocketHandler {
    private final Logger LOGGER = LogManager.getLogger(CoordinatorSocketHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, GameActionHandler> handlers;
    private final ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private final SessionManager sessionManager;

    public CoordinatorSocketHandler(
            List<GameActionHandler> handlerList,
            SessionManager sessions) {

        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(GameActionHandler::getAction, Function.identity()));
        this.sessionManager = sessions;
    }

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

                if (handler.mustBeAuthed() && !isAuthed(session)) {
                    sessionManager.sendObject(session, new ResultStatus("need_auth"));
                    return;
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
        if (session.getAttributes().containsKey("playerId"))
            sessionManager.removePlayer(session.getAttributes().get("playerId").toString());
        LOGGER.debug("Player disconnected: {}", session.getId());
    }

    private boolean isAuthed(WebSocketSession session) {
        if (session.getAttributes().get("playerId") != null)
            return true;
        return false;
    }
}
