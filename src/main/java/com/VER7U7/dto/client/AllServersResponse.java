package com.VER7U7.dto.client;

import java.util.List;

public record AllServersResponse(String status, List<GameServerInfo> all_servers) {
}
