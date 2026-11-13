package com.moviehub.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.moviehub.model.ErrorResponse;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

final class JsonResponses {

    static final String CONTENT_TYPE = "application/json; charset=UTF-8";
    static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private JsonResponses() {
    }

    static void send(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        send(exchange, status, new ErrorResponse(message));
    }

    static void sendError(HttpExchange exchange, int status, String message, List<String> details)
            throws IOException {
        send(exchange, status, new ErrorResponse(message, details));
    }

    static void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1);
    }
}
