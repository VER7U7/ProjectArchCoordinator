package com.VER7U7.websock.handlers.auth;

import com.VER7U7.auth.JwtService;
import com.VER7U7.dto.LoginResult;
import com.VER7U7.dto.Result;
import com.VER7U7.dto.ResultStatus;
import com.VER7U7.dto.TokensData;
import com.VER7U7.exceptions.AccountNotExists;
import com.VER7U7.exceptions.BadCredentialException;
import com.VER7U7.exceptions.BadTokenException;
import com.VER7U7.exceptions.OldTokenException;
import com.VER7U7.models.PlayerAccount;
import com.VER7U7.service.AuthService;
import com.VER7U7.utils.AuthTypes;
import com.VER7U7.utils.TraceUtils;
import com.VER7U7.websock.handlers.GameActionHandler;
import com.VER7U7.websock.sessions.SessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Component
public class AuthHandler implements GameActionHandler {
    private final Logger LOGGER = LogManager.getLogger(AuthHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final SessionManager sessionManager;


    private final AuthService authService;

    public AuthHandler(SessionManager sessionManager, AuthService authService) {
        this.sessionManager = sessionManager;

        this.authService = authService;
    }

    @Override
    public String getAction() {
        return "AUTH";
    }

    @Override
    public void sendMessage(WebSocketSession session, Object data) {
        sessionManager.sendObject(session, new Result("AUTH_RESULT", data));
    }

    @Override
    public void handle(WebSocketSession session, JsonNode data) throws Exception {

        if (!data.has("type")) {
            sendMessage(session, new ResultStatus("no_type_defined"));
            return;
        }

        LOGGER.debug(data.get("type").asText());
        AuthTypes type = AuthTypes.valueOf(data.get("type").asText());

        switch(type) {
            case CREATE -> HandleCreatingAccount(session, data);
            case AUTH -> HandleAuthentication(session, data);
            case LOGIN -> HandleLogin(session, data);
            default -> {
                sendMessage(session, new ResultStatus("no_type_defined"));
            }
        }
    }

    private void HandleCreatingAccount(WebSocketSession session, JsonNode data) {
        if (!data.has("nickname") && !data.has("password")) {
            sendMessage(session, new ResultStatus("creation_failed_bad_request"));
            return;
        }

        String name = data.get("nickname").asText();
        String password = data.get("password").asText();

        try {
            PlayerAccount account = authService.CreateAccount(name, password);

            TokensData tokens = authService.createSession(session, account);

            sendMessage(session, new LoginResult("auth_successful", tokens));
        }catch (BadCredentialException e) {
            switch(e.getType()) {
                case BadPassword -> sendMessage(session, new ResultStatus("auth_failed_bad_password"));
                case BadLogin -> sendMessage(session, new ResultStatus("auth_failed_bad_login"));
                case BadDefault -> sendMessage(session, new ResultStatus("auth_failed_bad_undefined"));
                case LoginIsExists -> sendMessage(session, new ResultStatus("auth_failed_login_is_exists"));
            }
            LOGGER.debug("Creation account error: {}", e.getMessage());
        }
    }

    private void HandleAuthentication(WebSocketSession session, JsonNode data) {
        if (!data.has("token")) {
            sendMessage(session, new ResultStatus("auth_failed_no_token"));
            return;
        }
        String token = data.get("token").asText();

        try {
            PlayerAccount account = authService.validateRefreshToken(token);

            WebSocketSession sessionBuffered = sessionManager.getPlayerSession(account.getId());

            if (sessionBuffered != null && !sessionBuffered.getId().equals(session.getId())) {
                sessionManager.kick(sessionBuffered, new ResultStatus("auth_another_login"));
            }

            TokensData tokens = authService.createSession(session, account);

            sendMessage(session, new LoginResult("auth_successful", tokens));

        } catch(IOException e) {
            LOGGER.debug(TraceUtils.printStackTrace(e));
        } catch (OldTokenException ex) {
            LOGGER.debug(TraceUtils.printStackTrace(ex));
            sendMessage(session, new ResultStatus("auth_failed_old_token"));
        } catch (AccountNotExists e) {
            sendMessage(session, new ResultStatus("auth_failed_account_not_exists"));
        } catch (BadTokenException e) {
            sendMessage(session, new ResultStatus("auth_failed_invalid_token"));
        }
    }

    private void HandleLogin(WebSocketSession session, JsonNode data) {

        if (session.getAttributes().containsKey("playerId")) {
            sendMessage(session, new ResultStatus("you_have_been_authenticated"));
            return;
        }

        String playerName = data.get("nickname").asText();
        String password = data.get("password").asText();

        PlayerAccount account = authService.loginAccount(playerName, password);
        if (account == null) {
            sendMessage(session, new ResultStatus("invalid_password_or_nickname"));
            return;
        }

        TokensData tokens = authService.createSession(session, account);

        sendMessage(session, new LoginResult("auth_successful", tokens));
    }

    @Override
    public boolean mustBeAuthed() {
        return false;
    }
}
