package com.moviehub.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.moviehub.model.Movie;
import com.moviehub.storage.MovieStorage;
import com.moviehub.validation.MovieValidator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

class MoviesHandler implements HttpHandler {

    private static final String COLLECTION_PATH = "/movies";
    private static final String ITEM_PREFIX = "/movies/";

    private final MovieStorage storage;

    MoviesHandler(MovieStorage storage) {
        this.storage = storage;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            route(exchange);
        } catch (RuntimeException e) {
            JsonResponses.sendError(exchange, 500, "Внутренняя ошибка сервера");
        } finally {
            exchange.close();
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String path = normalize(exchange.getRequestURI().getRawPath());
        String method = exchange.getRequestMethod();

        if (path.equals(COLLECTION_PATH)) {
            switch (method) {
                case "GET" -> handleList(exchange);
                case "POST" -> handleCreate(exchange);
                default -> methodNotAllowed(exchange, "GET, POST");
            }
            return;
        }

        if (path.startsWith(ITEM_PREFIX) && path.indexOf('/', ITEM_PREFIX.length()) < 0) {
            String rawId = path.substring(ITEM_PREFIX.length());
            switch (method) {
                case "GET" -> handleGet(exchange, rawId);
                case "DELETE" -> handleDelete(exchange, rawId);
                default -> methodNotAllowed(exchange, "GET, DELETE");
            }
            return;
        }

        JsonResponses.sendError(exchange, 404, "Ресурс не найден");
    }

    private String normalize(String path) {
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    private void methodNotAllowed(HttpExchange exchange, String allowed) throws IOException {
        exchange.getResponseHeaders().set("Allow", allowed);
        JsonResponses.sendError(exchange, 405, "Метод не поддерживается");
    }

    private void handleList(HttpExchange exchange) throws IOException {
        String rawQuery = exchange.getRequestURI().getRawQuery();
        String yearParam = findQueryParam(rawQuery, "year");

        if (yearParam == null) {
            JsonResponses.send(exchange, 200, storage.findAll());
            return;
        }

        Integer year = parseInt(yearParam);
        if (year == null) {
            JsonResponses.sendError(exchange, 400, "Некорректный параметр запроса — 'year'");
            return;
        }
        List<Movie> movies = storage.findByYear(year);
        JsonResponses.send(exchange, 200, movies);
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        if (!isJsonContentType(exchange.getRequestHeaders().getFirst("Content-Type"))) {
            JsonResponses.sendError(exchange, 415, "Неподдерживаемый тип содержимого");
            return;
        }

        String raw = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject body = parseObject(raw);
        if (body == null) {
            JsonResponses.sendError(exchange, 400, "Некорректный JSON");
            return;
        }

        List<String> errors = MovieValidator.validate(body);
        if (!errors.isEmpty()) {
            JsonResponses.sendError(exchange, 422, "Ошибка валидации", errors);
            return;
        }

        Movie movie = storage.add(body.get("title").getAsString(), body.get("year").getAsInt());
        JsonResponses.send(exchange, 201, movie);
    }

    private void handleGet(HttpExchange exchange, String rawId) throws IOException {
        Long id = parseLong(rawId);
        if (id == null) {
            JsonResponses.sendError(exchange, 400, "Некорректный ID");
            return;
        }
        Optional<Movie> movie = storage.findById(id);
        if (movie.isEmpty()) {
            JsonResponses.sendError(exchange, 404, "Фильм не найден");
            return;
        }
        JsonResponses.send(exchange, 200, movie.get());
    }

    private void handleDelete(HttpExchange exchange, String rawId) throws IOException {
        Long id = parseLong(rawId);
        if (id == null) {
            JsonResponses.sendError(exchange, 400, "Некорректный ID");
            return;
        }
        if (!storage.delete(id)) {
            JsonResponses.sendError(exchange, 404, "Фильм не найден");
            return;
        }
        JsonResponses.sendNoContent(exchange);
    }

    private boolean isJsonContentType(String header) {
        if (header == null) {
            return false;
        }
        String[] parts = header.split(";");
        if (!parts[0].trim().equalsIgnoreCase("application/json")) {
            return false;
        }
        for (int i = 1; i < parts.length; i++) {
            String[] pair = parts[i].split("=", 2);
            if (pair.length == 2 && pair[0].trim().equalsIgnoreCase("charset")) {
                String charset = pair[1].trim().replace("\"", "");
                if (!charset.equalsIgnoreCase("utf-8")) {
                    return false;
                }
            }
        }
        return true;
    }

    private JsonObject parseObject(String raw) {
        try {
            JsonElement element = JsonParser.parseString(raw);
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (JsonParseException e) {
            return null;
        }
    }

    private String findQueryParam(String rawQuery, String name) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return null;
        }
        for (String pair : rawQuery.split("&")) {
            String[] kv = pair.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            if (key.equals(name)) {
                return kv.length == 2 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            }
        }
        return null;
    }

    private Integer parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(URLDecoder.decode(value, StandardCharsets.UTF_8).trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
