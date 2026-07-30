package com.VER7U7.websock.handlers.client.auth;

import com.VER7U7.dto.client.LoginResponse;
import com.VER7U7.dto.client.StatusResponse;
import com.VER7U7.dto.common.TokenPair;
import com.VER7U7.dto.common.WsMessage;
import com.VER7U7.exceptions.AccountNotExists;
import com.VER7U7.exceptions.BadCredentialException;
import com.VER7U7.exceptions.BadTokenException;
import com.VER7U7.exceptions.OldTokenException;
import com.VER7U7.models.PlayerAccount;
import com.VER7U7.service.ClientAuthService;
import com.VER7U7.utils.TraceUtils;
import com.VER7U7.websock.handlers.client.GameActionHandler;
import com.VER7U7.websock.sessions.ClientSessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Component
public class AuthHandler implements GameActionHandler {
    private final Logger LOGGER = LogManager.getLogger(AuthHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ClientSessionManager sessionManager;


    private final ClientAuthService clientAuthService;

    public AuthHandler(ClientSessionManager sessionManager, ClientAuthService clientAuthService) {
        this.sessionManager = sessionManager;

        this.clientAuthService = clientAuthService;
    }

    @Override
    public String getAction() {
        return "AUTH";
    }


    @Override
    public void sendMessage(WebSocketSession session, Object data) {
        sessionManager.sendObject(session, new WsMessage<>("AUTH_RESULT", data));
    }


    /**
     * Processes all authentication requests from the client.
     * <p>
     * Determines the request type and delegates execution to the corresponding handler method.
     * If the request type is missing or unsupported, sends an error message back to the client.
     *
     * @param session the {@link WebSocketSession} assigned to the client
     * @param data the {@link JsonNode} containing request type and payload
     * */
    @Override
    public void handle(WebSocketSession session, JsonNode data) {

        if (!data.has("type")) {
            sendMessage(session, new StatusResponse("no_type_defined"));
            return;
        }

        LOGGER.debug(data.get("type").asText());
        AuthType type = AuthType.valueOf(data.get("type").asText());

        switch(type) {
            case CREATE -> handleCreatingAccount(session, data);
            case AUTH -> handleAuthentication(session, data);
            case LOGIN -> handleLogin(session, data);
            default -> {
                sendMessage(session, new StatusResponse("no_type_defined"));
            }
        }
    }


    /**
     * Processes an account creation request from the client.
     * <p>
     * Validates the provided nickname and password, registers the new account, and
     * automatically initializes a session for the player. Sends detailed error statuses
     * back to the client if the registration fails due to invalid or duplicate credentials.
     *
     * @param session the {@link WebSocketSession} assigned to the client
     * @param data the {@link JsonNode} containing the new player's credentials
     *
     * */
    private void handleCreatingAccount(WebSocketSession session, JsonNode data) {
        if (!data.has("nickname") && !data.has("password")) {
            sendMessage(session, new StatusResponse("creation_failed_bad_request"));
            return;
        }

        String name = data.get("nickname").asText();
        String password = data.get("password").asText();

        try {
            PlayerAccount account = clientAuthService.createAccount(name, password);

            TokenPair tokens = clientAuthService.createSession(session, account);

            sendMessage(session, new LoginResponse("auth_successful", tokens));
        }catch (BadCredentialException e) {
            switch(e.getType()) {
                case BadPassword -> sendMessage(session, new StatusResponse("auth_failed_bad_password"));
                case BadLogin -> sendMessage(session, new StatusResponse("auth_failed_bad_login"));
                case BadDefault -> sendMessage(session, new StatusResponse("auth_failed_bad_undefined"));
                case LoginIsExists -> sendMessage(session, new StatusResponse("auth_failed_login_is_exists"));
            }
            LOGGER.debug("Creation account error: {}", e.getMessage());
        }
    }


    /**
     * Processes an authentication request from the client.
     * <p>
     * Validates the provided token, terminates any existing stale sessions for the
     * player if necessary, and registers the new session. Sends a corresponding
     * status message back to the client depending on the validation outcome.
     *
     * @param session the {@link WebSocketSession} assigned to the client
     * @param data the {@link JsonNode} containing the refresh token
     * */
    private void handleAuthentication(WebSocketSession session, JsonNode data) {
        if (!data.has("token")) {
            sendMessage(session, new StatusResponse("auth_failed_no_token"));
            return;
        }
        String token = data.get("token").asText();

        try {
            PlayerAccount account = clientAuthService.validateRefreshToken(token);

            WebSocketSession sessionBuffered = sessionManager.getPlayerSession(account.getId());

            if (sessionBuffered != null && !sessionBuffered.getId().equals(session.getId())) {
                sessionManager.kick(sessionBuffered, new StatusResponse("auth_another_login"));
            }

            TokenPair tokens = clientAuthService.createSession(session, account);

            sendMessage(session, new LoginResponse("auth_successful", tokens));

        } catch(IOException e) {
            LOGGER.debug(TraceUtils.printStackTrace(e));
        } catch (OldTokenException ex) {
            LOGGER.debug(TraceUtils.printStackTrace(ex));
            sendMessage(session, new StatusResponse("auth_failed_old_token"));
        } catch (AccountNotExists e) {
            sendMessage(session, new StatusResponse("auth_failed_account_not_exists"));
        } catch (BadTokenException e) {
            sendMessage(session, new StatusResponse("auth_failed_invalid_token"));
        }
    }

    /**
     * Processes a login request from the client.
     * <p>
     * If the client is already authenticated, sends a warning message. Otherwise,
     * validates the credentials and returns either a success message with new tokens or an error status code.
     *
     * @param session the {@link WebSocketSession} assigned to the client
     * @param data the {@link JsonNode} containing the player's credentials
     * */
    private void handleLogin(WebSocketSession session, JsonNode data) {

        if (session.getAttributes().containsKey("playerId")) {
            sendMessage(session, new StatusResponse("you_have_been_authenticated"));
            return;
        }

        String playerName = data.get("nickname").asText();
        String password = data.get("password").asText();

        try {
            PlayerAccount account = clientAuthService.loginAccount(playerName, password);

            TokenPair tokens = clientAuthService.createSession(session, account);

            sendMessage(session, new LoginResponse("auth_successful", tokens));

        }catch(AccountNotExists | BadCredentialException ane) {
            sendMessage(session, new StatusResponse("invalid_password_or_nickname"));
        }
    }

    @Override
    public boolean mustBeAuthed() {
        return false;
    }
}
