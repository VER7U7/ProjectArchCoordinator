package com.VER7U7.data;

import com.VER7U7.utils.ServerRegion;

import java.util.Objects;

public class GameServerData {

    private String ip;
    private int port, max_players, current_players;
    private String sessionId;
    private ServerRegion region;

    public GameServerData(String ip, int port, int max_players, ServerRegion region, String sessionId) {
        this.ip = ip;
        this.port = port;
        this.max_players = max_players;
        this.region = region;
        this.sessionId = sessionId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getMax_players() {
        return max_players;
    }

    public void setMax_players(int max_players) {
        this.max_players = max_players;
    }

    public int getCurrent_players() {
        return current_players;
    }

    public void setCurrent_players(int current_players) {
        this.current_players = current_players;
    }

    public ServerRegion getRegion() {
        return region;
    }

    public void setRegion(ServerRegion region) {
        this.region = region;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GameServerData that)) return false;
        return port == that.port && max_players == that.max_players && current_players == that.current_players && Objects.equals(ip, that.ip) && Objects.equals(region, that.region) && Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, region, port, max_players, current_players, sessionId);
    }

    @Override
    public String toString() {
        return "GameServerData{" +
                "ip='" + ip + '\'' +
                ", region='" + region + '\'' +
                ", port=" + port +
                ", max_players=" + max_players +
                ", current_players=" + current_players +
                ", sessionId='" + sessionId + '\'' +
                '}';
    }
}
