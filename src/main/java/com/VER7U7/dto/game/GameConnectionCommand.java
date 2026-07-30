package com.VER7U7.dto.game;

import com.VER7U7.dto.common.ConnectionKey;
import com.VER7U7.utils.ConnectionType;

public record GameConnectionCommand(ConnectionType type, ConnectionKey connectionKey, String correlationId) {

    public GameConnectionCommand {
        type = ConnectionType.CONNECT;
    }

    public GameConnectionCommand(ConnectionKey key, String correlationId) {
        this(ConnectionType.CONNECT, key, correlationId);
    }
}
