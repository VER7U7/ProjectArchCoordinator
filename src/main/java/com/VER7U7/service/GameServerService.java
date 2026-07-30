package com.VER7U7.service;

import com.VER7U7.data.GameServerData;
import com.VER7U7.utils.ServerRegion;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
@Service
public class GameServerService {

    private final Map<ServerRegion, List<GameServerData>> gameServers = new ConcurrentHashMap<>();

    public void registerGameServer(GameServerData serverData) {
        gameServers.computeIfAbsent(serverData.getRegion(), k -> new ArrayList<>());
        gameServers.get(serverData.getRegion()).add(serverData);
    }

    public List<ServerRegion> getAllRegions() {
        return gameServers.keySet().stream().toList();
    }

    public List<GameServerData> getAllGameServers(ServerRegion region) {
        return gameServers.get(region);
    }

    public Optional<GameServerData> getFirstFreeGameServer(ServerRegion region) {
        List<GameServerData> allServers = gameServers.get(region);
        if (allServers == null || allServers.isEmpty()) {
            return Optional.empty();
        }

        return allServers.stream()
                .filter(server -> server.getMax_players() > server.getCurrent_players())
                .max(Comparator.comparingInt(GameServerData::getCurrent_players));
    }

    public Optional<GameServerData> getDataBySessionId(String sessionId) {
        return gameServers.values().stream()
                .flatMap(List::stream)
                .filter(server -> sessionId.equals(server.getSessionId()))
                .findFirst();
    }
}
