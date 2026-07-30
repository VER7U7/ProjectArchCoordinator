package com.VER7U7.dto.client;

import com.VER7U7.dto.common.TokenPair;

public record LoginResponse(String status, TokenPair tokens) {
}
