package com.VER7U7.websock.handlers.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.socket.WebSocketSession;

public interface GameActionHandler {
    String getAction();

    void sendMessage(WebSocketSession session, Object data);

    void handle(WebSocketSession session, JsonNode data) throws Exception;
    boolean mustBeAuthed();
}
