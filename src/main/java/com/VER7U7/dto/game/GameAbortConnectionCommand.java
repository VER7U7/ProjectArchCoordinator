package com.VER7U7.dto.game;

import com.VER7U7.utils.ConnectionType;

public record GameAbortConnectionCommand(ConnectionType type, String secret) {
    public GameAbortConnectionCommand {
        type = ConnectionType.ABORT;
    }

    public GameAbortConnectionCommand(String secret) {
        this(ConnectionType.ABORT, secret);
    }
}
