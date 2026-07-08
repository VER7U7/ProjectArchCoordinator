package com.VER7U7.websock.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class FindServerHandler implements GameActionHandler {
    @Override
    public String getAction() {
        return "FIND_SERVER";
    }

    @Override
    public void handle(WebSocketSession session, JsonNode data) throws Exception {
        String response = "{\"ip\": \"127.0.0.1\", \"port\": 7778 }";
        session.sendMessage(new TextMessage(response));
    }

    @Override
    public boolean mustBeAuthed() {
        return true;
    }
}
