package com.VER7U7.websock.handlers;

import com.VER7U7.data.GameServerData;
import com.VER7U7.dto.common.WsMessage;
import com.VER7U7.dto.game.GameServerAuthResponse;
import com.VER7U7.dto.client.StatusResponse;
import com.VER7U7.service.GameServerService;
import com.VER7U7.service.ServerRpcManager;
import com.VER7U7.utils.ServerRegion;
import com.VER7U7.websock.handlers.server.ServerActionHandler;
import com.VER7U7.websock.sessions.ServerSessionManager;
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
public class ServerCoordinator extends TextWebSocketHandler {

    private static final Logger LOGGER = LogManager.getLogger(ServerCoordinator.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, ServerActionHandler> handlers;
    private final ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ServerSessionManager sessionManager;
    private final GameServerService gameServerService;
    private final ServerRpcManager serverRpcManager;

    public ServerCoordinator(
            List<ServerActionHandler> handlerList,
            ServerSessionManager sessionManager,
            GameServerService gameServerService, ServerRpcManager serverRpcManager) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(ServerActionHandler::getAction, Function.identity()));
        this.sessionManager = sessionManager;
        this.gameServerService = gameServerService;
        this.serverRpcManager = serverRpcManager;
    }


    /**
     * Processes incoming WebSocket text messages from game servers and
     * routes them to the appropriate action handlers.
     * <p>
     * It also checks each message against the {@code correlationId}. If
     * a {@code correlationId} is present, it completes the pending
     * RPC request via {@link ServerRpcManager}.
     *
     * @param session the {@link WebSocketSession} assigned to the client
     * @param message the {@link TextMessage} containing the request data in {@code JSON} format
     * @throws Exception if an error occurs during message processing
     **/
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        try {
            JsonNode jsonNode = objectMapper.readTree(payload);

            if (!jsonNode.has("action")) {
                return;
            }

            String action = jsonNode.get("action").asText();
            JsonNode data = jsonNode.has("data") ? jsonNode.get("data") : objectMapper.createObjectNode();

            if (action.equals("AUTH")) {
                handleAuthServer(session, data);
                return;
            }

            if (!sessionManager.validateSession(session)) {
                sessionManager.sendObject(session, new WsMessage<>( "AUTH", new StatusResponse("need_initialization") ));
                return;
            }

            if (data.has("correlationId")) {
                serverRpcManager.completeRequest(data.get("correlationId").asText(), data);
                return;
            }

            ServerActionHandler handler = handlers.get(action);
            handler.handle(session, data);
        } finally {
            LOGGER.debug("Message from {}: {}", session.getId(), payload);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        LOGGER.debug("Server connected, waiting initialization message. Session ID: {}", session.getId());

        sessionManager.sendObject(session, new WsMessage<>(
                "AUTH",
                new StatusResponse("need_initialization")
        ));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
    }


    /**
     * Handles authentication requests from the game servers.
     * <p>
     * Validates incoming payload data, creates a {@link GameServerData} instance,
     * and registers it with the {@link GameServerService}. Then sends a response
     * back to the game server with its session id.
     *
     * @param session the {@link WebSocketSession} assigned to the client
     * @param data the {@link JsonNode} containing the authentication request payload
     **/
    private void handleAuthServer(WebSocketSession session, JsonNode data) {
        if (sessionManager.validateSession(session))
            return;

        if (!data.has("port")
                || !data.has("max_players")
                || !data.has("region")) {
            sessionManager.sendObject(session, new WsMessage<>("AUTH", new StatusResponse("invalid_payload")));
            return;
        }

        String ip = session.getRemoteAddress().getAddress().toString();
        ServerRegion region = ServerRegion.valueOf(data.get("region").asText());
        int port = data.get("port").asInt();
        int max_players = data.get("max_players").asInt();

        GameServerData serverData = new GameServerData(ip, port, max_players, region, session.getId());

        gameServerService.registerGameServer(serverData);

        sessionManager.registerServer(session.getId(), session);
        sessionManager.sendObject(session,
                new WsMessage<>(
                        "AUTH",
                        new GameServerAuthResponse(
                                "auth_success",
                                session.getId()
                        )
                ));
    }
}
