package com.VER7U7.websock.handlers.auth;

import com.VER7U7.auth.JwtService;
import com.VER7U7.dto.LoginResult;
import com.VER7U7.dto.ResultStatus;
import com.VER7U7.utils.AuthTypes;
import com.VER7U7.websock.handlers.GameActionHandler;
import com.VER7U7.websock.sessions.SessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class AuthHandler implements GameActionHandler {
    private final Logger LOGGER = LogManager.getLogger(AuthHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final SessionManager sessionManager;
    private final JwtService jwtService;

    public AuthHandler(SessionManager sessionManager, JwtService jwtService) {
        this.sessionManager = sessionManager;
        this.jwtService = jwtService;
    }

    @Override
    public String getAction() {
        return "AUTH";
    }

    @Override
    public void handle(WebSocketSession session, JsonNode data) throws Exception {

        if (!data.has("type")) {
            sessionManager.sendObject(session, new ResultStatus("no_type_defined"));
            return;
        }

        LOGGER.debug(data.get("type").asText());
        AuthTypes type = AuthTypes.valueOf(data.get("type").asText());

        switch(type) {
            case CREATE -> HandleCreatingAccount(session, data);
            case AUTH -> HandleAuthentication(session, data);
            case LOGIN -> HandleLogin(session, data);
            default -> {
                sessionManager.sendObject(session, new ResultStatus("no_type_defined"));
            }
        }
    }

    private void HandleCreatingAccount(WebSocketSession session, JsonNode data) {

    }

    private void HandleAuthentication(WebSocketSession session, JsonNode data) {
        if (!data.has("token")) {
            sessionManager.sendObject(session, new ResultStatus("auth_failed_no_token"));
            return;
        }
        String token = data.get("token").asText();

        String playerId = jwtService.validateTokenAndGetPlayerId(token);

        if (playerId == null) {
            sessionManager.sendObject(session, new ResultStatus("auth_failed_invalid_token"));
            return;
        }

        session.getAttributes().put("playerId", playerId);
        sessionManager.registerPlayer(playerId, session);

        sessionManager.sendObject(session, new ResultStatus("auth_ok"));
    }

    private void HandleLogin(WebSocketSession session, JsonNode data) {

        if (session.getAttributes().containsKey("playerId")) {
            sessionManager.sendObject(session, new ResultStatus("you_have_been_authenticated"));
            return;
        }

        String playerId = data.get("playerId").asText();
        session.getAttributes().put("playerId", playerId);
        sessionManager.registerPlayer(playerId, session);

        String newToken = jwtService.generateToken(playerId);

        sessionManager.sendObject(session, new LoginResult("auth_ok", newToken));
    }

    @Override
    public boolean mustBeAuthed() {
        return false;
    }
}
