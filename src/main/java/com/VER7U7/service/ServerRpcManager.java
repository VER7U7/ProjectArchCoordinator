package com.VER7U7.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class ServerRpcManager {
    private final ConcurrentMap<String, CompletableFuture<JsonNode>> pendingRequests = new ConcurrentHashMap<>();

    /**
     * Adds a {@code correlationId} to the pending requests map and returns {@link CompletableFuture}
     * which will be processed when completed by {@link #completeRequest(String, JsonNode)}
     *
     * @param correlationId the ID assigned to a pending response
     * @return the newly created {@link CompletableFuture} instance
     * @see #completeRequest
     **/
    public CompletableFuture<JsonNode> createPendingRequest(String correlationId) {
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pendingRequests.put(correlationId, future);
        return future;
    }

    /**
     * Completes the {@link CompletableFuture} which is contained in the pending requests map
     * and removes it.
     *
     * @param correlationId the ID assigned to a pending response
     * @param responsePayload the {@link JsonNode} containing the payload
     **/
    public void completeRequest(String correlationId, JsonNode responsePayload) {
        CompletableFuture<JsonNode> future = pendingRequests.remove(correlationId);
        if (future != null) {
            future.complete(responsePayload);
        }
    }
}
