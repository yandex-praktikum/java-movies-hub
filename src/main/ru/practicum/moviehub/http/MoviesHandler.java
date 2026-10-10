package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;

import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import ru.practicum.moviehub.api.ErrorResponse;

public class MoviesHandler extends BaseHttpHandler {

    private final MoviesStore moviesStore;
    private final Gson gson = new Gson();

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();

        if (method.equalsIgnoreCase("GET")) {
            if (path.equals("/movies")) {
                handleGetMovies(ex);
            } else if (path.startsWith("/movies/")) {
                String id = path.substring("/movies/".length());
                handleGetById(ex, id);
            } else {
                sendError(ex, 404, "Ресурс не найден", null);
            }
        } else if (method.equalsIgnoreCase("POST")) {
            if (path.equals("/movies")) {
                handlePost(ex);
            } else {
                sendError(ex, 404, "Ресурс не найден", null);
            }
        } else if (method.equalsIgnoreCase("DELETE")) {
            if (path.startsWith("/movies/")) {
                String id = path.substring("/movies/".length());
                handleDelete(ex, id);
            } else {
                sendError(ex, 405, "Метод не поддерживается", null);
            }
        } else {
            sendError(ex, 405, "Метод не поддерживается", null);
        }
    }

    private void handlePost(HttpExchange ex) throws IOException {
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");

        if (contentType == null
                || !contentType.split(";")[0].trim()
                .equalsIgnoreCase("application/json")) {
            sendError(ex, 415, "Ожидается Content-Type application/json", null);
            return;
        }

        String body = new String(
                ex.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );

        NewMovieRequest request;

        try {
            request = gson.fromJson(body, NewMovieRequest.class);
        } catch (JsonParseException e) {
            sendError(ex, 400, "Некорректный JSON", null);
            return;
        }

        if (request == null) {
            sendError(ex, 400, "Некорректный JSON", null);
            return;
        }

        List<String> details = new ArrayList<>();

        if (request.title == null || request.title.isBlank()) {
            details.add("Название фильма не должно быть пустым");
        } else if (request.title.length() > 100) {
            details.add("Название фильма не должно превышать 100 символов");
        }

        int currentYear = Year.now().getValue();

        if (request.year == null) {
            details.add("Не указан год выпуска");
        } else if (request.year < 1888 || request.year > currentYear + 1) {
            details.add("Год выпуска должен быть от 1888 до " + (currentYear + 1));
        }

        if (!details.isEmpty()) {
            sendError(ex, 422, "Ошибка валидации", details);
            return;
        }

        Movie movie = moviesStore.add(request.title, request.year);
        sendJson(ex, 201, gson.toJson(movie));
    }

    private void sendError(
            HttpExchange ex,
            int status,
            String error,
            List<String> details
    ) throws IOException {
        ErrorResponse response = new ErrorResponse(error, details);
        sendJson(ex, status, gson.toJson(response));
    }

    private static class NewMovieRequest {
        String title;
        Integer year;
    }


    private void handleGetMovies(HttpExchange ex) throws IOException {
        String query = ex.getRequestURI().getRawQuery();

        if (query == null) {
            sendJson(ex, 200, gson.toJson(moviesStore.findAll()));
            return;
        }

        if (!query.startsWith("year=") || query.contains("&")) {
            sendError(ex, 400,
                    "Некорректный параметр запроса — 'year'", null);
            return;
        }

        int year;

        try {
            year = Integer.parseInt(query.substring(5));
        } catch (NumberFormatException e) {
            sendError(ex, 400,
                    "Некорректный параметр запроса — 'year'", null);
            return;
        }

        int currentYear = Year.now().getValue();

        if (year < 1888 || year > currentYear + 1) {
            sendError(ex, 400,
                    "Некорректный параметр запроса — 'year'", null);
            return;
        }

        List<Movie> filteredMovies = new ArrayList<>();

        for (Movie movie : moviesStore.findAll()) {
            if (movie.getYear() == year) {
                filteredMovies.add(movie);
            }
        }

        sendJson(ex, 200, gson.toJson(filteredMovies));
    }

    private void handleGetById(HttpExchange ex, String idValue)
            throws IOException {
        int id;

        try {
            id = Integer.parseInt(idValue);
        } catch (NumberFormatException e) {
            sendError(ex, 400, "Некорректный ID", null);
            return;
        }

        Movie movie = moviesStore.findById(id);

        if (movie == null) {
            sendError(ex, 404, "Фильм не найден", null);
            return;
        }

        sendJson(ex, 200, gson.toJson(movie));
    }

    private void handleDelete(HttpExchange ex, String idValue)
            throws IOException {
        int id;

        try {
            id = Integer.parseInt(idValue);
        } catch (NumberFormatException e) {
            sendError(ex, 400, "Некорректный ID", null);
            return;
        }

        Movie deletedMovie = moviesStore.delete(id);

        if (deletedMovie == null) {
            sendError(ex, 404, "Фильм не найден", null);
            return;
        }

        sendNoContent(ex);
    }

}
