package com.VER7U7.websock.handlers.client.match;

import com.VER7U7.data.GameServerData;
import com.VER7U7.dto.client.AllServersResponse;
import com.VER7U7.dto.client.FindServerResponse;
import com.VER7U7.dto.client.GameServerInfo;
import com.VER7U7.dto.client.StatusResponse;
import com.VER7U7.dto.common.ConnectionKey;
import com.VER7U7.dto.common.WsMessage;
import com.VER7U7.dto.game.GameAbortConnectionCommand;
import com.VER7U7.dto.game.GameConnectionCommand;
import com.VER7U7.dto.game.GameErrorResponse;
import com.VER7U7.models.PlayerAccount;
import com.VER7U7.repo.PlayerAccountRepository;
import com.VER7U7.service.GameServerService;
import com.VER7U7.service.ServerRpcManager;
import com.VER7U7.utils.ConnectionType;
import com.VER7U7.utils.ServerRegion;
import com.VER7U7.utils.TraceUtils;
import com.VER7U7.websock.handlers.client.GameActionHandler;
import com.VER7U7.websock.sessions.ClientSessionManager;
import com.VER7U7.websock.sessions.ServerSessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class FindServerHandler implements GameActionHandler {
    private static final Logger LOGGER = LogManager.getLogger(FindServerHandler.class);

    private final ClientSessionManager sessionManager;
    private final GameServerService gameServers;
    private final ServerSessionManager serverManager;
    private final ServerRpcManager serverRpcManager;

    private final PlayerAccountRepository accountRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    public FindServerHandler(
            ClientSessionManager sessionManager,
            GameServerService gameServerService,
            ServerSessionManager serverSessionManager,
            ServerRpcManager serverRpcManager, PlayerAccountRepository accountRepository) {
        this.sessionManager = sessionManager;
        this.gameServers = gameServerService;
        this.serverManager = serverSessionManager;
        this.serverRpcManager = serverRpcManager;
        this.accountRepository = accountRepository;
    }

    @Override
    public String getAction() {
        return "FIND_SERVER";
    }

    @Override
    public void sendMessage(WebSocketSession session, Object data) {
        sessionManager.sendObject(session, new WsMessage<>("FIND_SERVER_RESULT", data));
    }


    /**
     * Handles {@code FindServer} requests, extracts the request type and
     * delegates execution to the corresponding handler methods.
     *
     * @param session the {@link WebSocketSession} assigned to the client
     * @param data the {@link JsonNode} containing the payload
     * @throws Exception if an error occurs during request processing
     **/
    @Override
    public void handle(WebSocketSession session, JsonNode data) throws Exception {
        try {
            if (!data.has("type")) {
                sendMessage(session, new StatusResponse("no_type_defined"));
                return;
            }

            LOGGER.debug(data.get("type").asText());
            FindServerType type = FindServerType.valueOf(data.get("type").asText());

            switch(type) {
                case ALL_SERVERS -> HandleAllServers(session, data);
                case CONNECT_TO -> HandleConnectTo(session, data);
                default -> sendMessage(session, new StatusResponse("no_type_defined"));
            }
        }catch(IllegalArgumentException iea) {
            LOGGER.debug("No type defined. {}", iea.getMessage());
            sendMessage(session, new StatusResponse("no_type_defined"));
        }
    }

    /**
     * Handles all requests with the {@link FindServerType#ALL_SERVERS} type.
     * <p>
     * After finds all free servers with defined region and return it.
     *
     * @param session the {@link WebSocketSession} assigned to the client
     * @param data the {@link JsonNode} containing the payload
     **/
    private void HandleAllServers(WebSocketSession session, JsonNode data) {
        if (!data.has("region")) {
            sendMessage(session, new StatusResponse("no_region_defined"));
            return;
        }

        try {
            ServerRegion region = ServerRegion.valueOf(data.get("region").asText());

            List<GameServerData> allServers = gameServers.getAllGameServers(region);
            if (allServers == null) {
                sendMessage(session, new StatusResponse("no_servers_find"));
                return;
            }

            List<GameServerInfo> result = new ArrayList<>();

            allServers.forEach(n -> result.add(
                    new GameServerInfo(
                            n.getRegion().name(),
                            n.getMax_players(),
                            n.getCurrent_players(),
                            n.getSessionId()
                    )
            ));
            sendMessage(session, new AllServersResponse("all_servers_success", result));
        } catch (IllegalArgumentException iea) {
            LOGGER.debug("No defined region. {}", iea.getMessage());
            sendMessage(session, new StatusResponse("no_region_defined"));
        }
    }


    /**
     * Handles all requests with the {@link FindServerType#CONNECT_TO} type.
     * <p>
     * Validates the request and searches for the assigned server by {@code serverId}. If the server
     * is found, generates a secure key and sends a request to the game server to reserve a place.
     * <p>
     * The response from the game server is processed asynchronously by
     * {@link #processAfterGameServerResponse(WebSocketSession, WebSocketSession, JsonNode)}.
     *
     * @param session the {@link WebSocketSession} assigned to the client
     * @param data the {@link JsonNode} containing the payload
     * @see #processAfterGameServerResponse
     */
    private void HandleConnectTo(WebSocketSession session, JsonNode data) {
        if (!data.has("serverId")) {
            sendMessage(session, "no_server_id_defined");
            return;
        }

        String serverId = data.get("serverId").asText();
        Optional<GameServerData> serverData = gameServers.getDataBySessionId(serverId);

        if (serverData.isEmpty()) {
            sendMessage(session, "no_server_exists");
            return;
        }

        try {
            String playerId = session.getAttributes().get("playerId").toString();
            Optional<PlayerAccount> account = accountRepository.findById(Long.parseLong(playerId));

            if (account.isEmpty()) {
                sendMessage(session, "internal_error_validate_key");
                return;
            }

            ConnectionKey connectionKey = new ConnectionKey(
                    account.get().getId(), session.getId() + LocalDateTime.now() + new Random().nextLong()
            );

            String correlationId = UUID.randomUUID().toString();
            CompletableFuture<JsonNode> response = serverRpcManager.createPendingRequest(correlationId);

            WebSocketSession serverSession = serverManager.getServerSession(serverData.get().getSessionId());
            serverManager.sendObject(serverSession, new WsMessage<>("CONNECTION",
                    new GameConnectionCommand(
                            ConnectionType.CONNECT,
                            connectionKey,
                            correlationId
                    )
            ));

            response
                    .orTimeout(5, TimeUnit.SECONDS)
                    .thenAccept(serverResponse -> processAfterGameServerResponse(session, serverSession, serverResponse))
                    .exceptionally(ex -> {
                        LOGGER.error("Game server didn't response in time. Operation cancelled.");
                        serverRpcManager.completeRequest(correlationId, null);
                        sendMessage(session, new StatusResponse("connection_cancelled_internal_error"));
                        return null;
                    });

        }catch(NumberFormatException num) {
            LOGGER.debug("Error with parsing id: {} for {}", num.getMessage(), session.getId());
        } catch(Exception e) {
            LOGGER.error(TraceUtils.printStackTrace(e));
        }

    }

    /**
     * Processes the response received from the game server after a reservation request.
     * <p>
     * Validates the server response status. If a place was reserved, sends the server
     * connection details and secret key to the client. Otherwise, notifies the client
     * about the error or lack of space.
     *
     * @param clientSession the {@link WebSocketSession} of the requesting client
     * @param serverSession the {@link WebSocketSession} of the game server
     * @param data the {@link JsonNode} containing the response from the game server
     * @see #HandleConnectTo
     */
    private void processAfterGameServerResponse(WebSocketSession clientSession, WebSocketSession serverSession, JsonNode data) {
        if (!data.has("status")) {
            serverManager.sendObject(serverSession, new WsMessage<>("CONNECTION",
                    new GameErrorResponse(ConnectionType.ERROR, "no_vars_defined")
            ));
            sendMessage(clientSession, new StatusResponse("internal_error_try_again"));
            LOGGER.debug("No defined variables in server response {}", serverSession.getId());
            return;
        }

        String status = data.get("status").asText();

        switch(status) {
            case "place_reserved":
                if (!data.has("secret")) {
                    serverManager.sendObject(serverSession, new WsMessage<>("CONNECTION",
                            new GameErrorResponse(ConnectionType.ERROR, "no_secret_defined")
                    ));
                    sendMessage(clientSession, new StatusResponse("internal_error_try_again"));
                    return;
                }

                String secretKey = data.get("secret").asText();
                Optional<GameServerData> serverData = gameServers.getDataBySessionId(serverSession.getId());

                if (serverData.isEmpty()) {
                    sendMessage(clientSession, new StatusResponse("internal_error_try_again"));
                    serverManager.sendObject(serverSession, new WsMessage<>("CONNECTION", new GameAbortConnectionCommand(ConnectionType.ABORT, secretKey)));
                    return;
                }

                sendMessage(clientSession, new FindServerResponse(
                        "connection_allowed",
                        serverData.get().getIp(),
                        serverData.get().getPort(),
                        secretKey
                ));

                return;
            case "no_enough_space":
                sendMessage(clientSession, new StatusResponse("no_enough_space"));
                return;
            default:
                LOGGER.debug("No status defined: {}", status);
                sendMessage(clientSession, new StatusResponse("internal_error_try_again"));
        }

    }

    @Override
    public boolean mustBeAuthed() {
        return true;
    }
}
