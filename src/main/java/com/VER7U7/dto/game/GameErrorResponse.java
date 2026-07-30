package com.VER7U7.dto.game;

import com.VER7U7.utils.ConnectionType;

public record GameErrorResponse(ConnectionType type, String status) {

    public GameErrorResponse {
        type = ConnectionType.ERROR;
    }

    public GameErrorResponse(String status) {
        this(ConnectionType.ERROR, status);
    }
}
