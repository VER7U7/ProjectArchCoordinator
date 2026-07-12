package com.VER7U7.websock.handlers;

import com.VER7U7.auth.JwtService;
import com.VER7U7.dto.Result;
import com.VER7U7.dto.ResultFindServer;
import com.VER7U7.websock.sessions.SessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class FindServerHandler implements GameActionHandler {

    private final SessionManager sessionManager;

    public FindServerHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public String getAction() {
        return "FIND_SERVER";
    }

    @Override
    public void sendMessage(WebSocketSession session, Object data) {
        sessionManager.sendObject(session, new Result("FIND_SERVER_RESULT", data));
    }

    @Override
    public void handle(WebSocketSession session, JsonNode data) throws Exception {
        sendMessage(session, new ResultFindServer("127.0.0.1", 7778));
    }

    @Override
    public boolean mustBeAuthed() {
        return true;
    }
}
