package com.VER7U7.websock.handlers;

import com.VER7U7.dto.Result;
import com.VER7U7.dto.ResultStatus;
import com.VER7U7.models.PlayerAccount;
import com.VER7U7.repo.PlayerAccountRepository;
import com.VER7U7.service.AuthService;
import com.VER7U7.websock.sessions.SessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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
    //private final Map<WebSocketSession, List<String>> requestBuffer = new LinkedHashMap<>(); //need for buffering all request when need refresh auth
    private final ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    private PlayerAccountRepository accountRepos;

    private AuthService authService;

    private final SessionManager sessionManager;

    public SocketCoordinator(
            List<GameActionHandler> handlerList,
            SessionManager sessions,
            AuthService authService) {
        this.authService = authService;
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
                if (handler.mustBeAuthed()) {
                    if (!jsonNode.has("accessToken")) {
                        sessionManager.sendObject(session, new Result("AUTH_RESULT", new ResultStatus("need_auth")));
                        return;
                    }

                    String accessToken = jsonNode.get("accessToken").asText();

                    if (!authService.validateAccessToken(session, accessToken)) {
                        sessionManager.sendObject(session, new Result("AUTH_RESULT", new ResultStatus("need_refresh_token")));
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

    /*private void addRequestToBuffer(WebSocketSession session, String payload) {
        if (requestBuffer.containsKey(session))
            requestBuffer.get(session).add(payload);
        else {
            requestBuffer.put(session, new ArrayList<>(List.of(payload)));
        }
    }*/

    private boolean isAuthed(WebSocketSession session) {
        return session.getAttributes().get("playerId") != null;
    }
}
