package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8";
    protected static Gson gson = new GsonBuilder().create();

    public void sendJson(HttpExchange ex, int status, String json) throws IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(status, 0);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    protected void sendNoContent(HttpExchange ex) throws java.io.IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(204, -1);
    }

    protected void sendMethodNotAllowed(HttpExchange ex) throws java.io.IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(405, -1);
    }

    public static boolean isInteger(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    protected void sendMovieNotFound(HttpExchange ex) throws java.io.IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        String error = gson.toJson(new ErrorResponse("Ошибка валидации",
                new String[]{"Фильм с данным айди не найден"}));
        ex.sendResponseHeaders(404, 0);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(error.getBytes(StandardCharsets.UTF_8));
        }
    }

    protected void sendUncorrectedId(HttpExchange ex) throws java.io.IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        String error = gson.toJson(new ErrorResponse("Ошибка валидации",
                new String[]{"Некорректный ID"}));
        ex.sendResponseHeaders(400, 0);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(error.getBytes(StandardCharsets.UTF_8));
        }
    }
}