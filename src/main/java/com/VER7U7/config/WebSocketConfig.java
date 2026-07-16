package com.VER7U7.config;

import com.VER7U7.websock.handlers.ServerCoordinator;
import com.VER7U7.websock.handlers.SocketCoordinator;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SocketCoordinator socketHandler;
    private final ServerCoordinator serverHandler;

    public WebSocketConfig(
            SocketCoordinator socketHandler,
            ServerCoordinator serverHandler) {
        this.socketHandler = socketHandler;
        this.serverHandler = serverHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(socketHandler, "/ws/coordinator")
                .addHandler(serverHandler, "/ws/server")
                .setAllowedOrigins("*");
    }
}
