package com.VER7U7.dto.common;

public record WsMessage<T>(String action, T data) {
}
