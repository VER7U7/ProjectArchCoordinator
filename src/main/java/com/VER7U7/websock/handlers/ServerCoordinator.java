package com.VER7U7.websock.handlers;

import com.VER7U7.proto.RegisterResponse;
import com.VER7U7.proto.ServerMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.util.UUID;

@Component
public class ServerCoordinator extends BinaryWebSocketHandler {
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        // 1. Достаем байты и парсим Protobuf
        byte[] payload = message.getPayload().array();
        ServerMessage incomingMsg = ServerMessage.parseFrom(payload);

        // 2. Роутер: смотрим, что внутри
        switch (incomingMsg.getPayloadCase()) {
            case REGISTER_REQUEST:
                handleRegister(session, incomingMsg.getRegisterRequest());
                break;
            default:
                System.out.println("Unknown message type");
        }
    }

    private void handleRegister(WebSocketSession session, com.VER7U7.proto.RegisterRequest req) throws Exception {
        System.out.println("New Unity server: " + req.getIp() + ":" + req.getPort());

        // Формируем ответ
        ServerMessage response = ServerMessage.newBuilder()
                .setRegisterResponse(RegisterResponse.newBuilder()
                        .setSuccess(true)
                        .setServerId(UUID.randomUUID().toString())
                        .build())
                .build();

        // Отправляем бинарный пакет обратно
        session.sendMessage(new BinaryMessage(response.toByteArray()));
    }
}
