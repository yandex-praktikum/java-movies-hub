package ru.practicum.moviehub.http;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class MoviesServer extends BaseHttpHandler {

    private final HttpServer server;
    private final MoviesStore store;

    public MoviesServer(MoviesStore store, int port) {
        this.store = store;

        try {
            server = HttpServer.create(
                    new InetSocketAddress("localhost", port),
                    0
            );

            server.createContext(
                    "/movies",
                    this
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "Не удалось создать сервер",
                    e
            );
        }
    }

    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        String method = exchange.getRequestMethod();
        URI uri = exchange.getRequestURI();
        String path = uri.getPath();

        if ("/movies".equals(path)) {
            handleMoviesCollection(exchange, method);
            return;
        }

        if (path.startsWith("/movies/")) {
            handleMovieById(exchange, method, path);
            return;
        }

        sendError(
                exchange,
                404,
                "Не найдено"
        );
    }

    public void start() {
        server.start();

        System.out.println(
                "MovieHub запущен на http://localhost:8080"
        );
    }

    public void stop() {
        server.stop(0);
    }

    private void handleMoviesCollection(
            HttpExchange exchange,
            String method
    ) throws IOException {

        switch (method) {
            case "GET":
                getMovies(exchange);
                break;

            case "POST":
                addMovie(exchange);
                break;

            default:
                sendError(
                        exchange,
                        405,
                        "Метод не поддерживается"
                );
        }
    }

    private void getMovies(
            HttpExchange exchange
    ) throws IOException {

        String query =
                exchange.getRequestURI().getQuery();

        if (query == null || query.isEmpty()) {
            List<Movie> movies = store.findAll();

            sendJsonResponse(
                    exchange,
                    200,
                    gson.toJson(movies)
            );

            return;
        }

        Integer year = extractYear(query);

        if (year == null) {
            sendError(
                    exchange,
                    400,
                    "Некорректный параметр запроса — 'year'"
            );

            return;
        }

        List<Movie> movies =
                store.findByYear(year);

        sendJsonResponse(
                exchange,
                200,
                gson.toJson(movies)
        );
    }

    private Integer extractYear(String query) {

        String[] params = query.split("&");

        for (String param : params) {

            String[] pair =
                    param.split("=", 2);

            if (pair.length == 2
                    && "year".equals(pair[0])) {

                try {
                    String value =
                            URLDecoder.decode(
                                    pair[1],
                                    StandardCharsets.UTF_8
                            );

                    return Integer.parseInt(value);

                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        return null;
    }

    private void addMovie(
            HttpExchange exchange
    ) throws IOException {

        String contentType =
                exchange.getRequestHeaders()
                        .getFirst("Content-Type");

        if (contentType == null
                || !contentType
                .toLowerCase()
                .startsWith("application/json")) {

            sendError(
                    exchange,
                    415,
                    "Неподдерживаемый Content-Type"
            );

            return;
        }

        String body = readBody(exchange);

        Movie movie;

        try {
            movie = gson.fromJson(
                    body,
                    Movie.class
            );

        } catch (JsonSyntaxException e) {

            sendValidationError(
                    exchange,
                    List.of("Некорректный JSON")
            );

            return;
        }

        if (movie == null) {

            sendValidationError(
                    exchange,
                    List.of("Некорректный JSON")
            );

            return;
        }

        List<String> details =
                validateMovie(movie);

        if (!details.isEmpty()) {

            sendValidationError(
                    exchange,
                    details
            );

            return;
        }

        movie = store.add(
                movie.getTitle(),
                movie.getYear()
        );

        sendJsonResponse(
                exchange,
                201,
                gson.toJson(movie)
        );
    }

    private List<String> validateMovie(
            Movie movie
    ) {

        List<String> details =
                new ArrayList<>();

        String title = movie.getTitle();

        if (title == null
                || title.trim().isEmpty()) {

            details.add(
                    "название не должно быть пустым"
            );

        } else if (title.length() > 100) {

            details.add(
                    "название не должно быть длиннее 100 символов"
            );
        }

        int currentYear =
                Year.now().getValue();

        if (movie.getYear() < 1888
                || movie.getYear() > currentYear + 1) {

            details.add(
                    "год должен быть между 1888 и "
                            + (currentYear + 1)
            );
        }

        return details;
    }

    private void handleMovieById(
            HttpExchange exchange,
            String method,
            String path
    ) throws IOException {

        String idValue =
                path.substring("/movies/".length());

        int id;

        try {
            id = Integer.parseInt(idValue);

        } catch (NumberFormatException e) {

            sendError(
                    exchange,
                    400,
                    "Некорректный ID"
            );

            return;
        }

        switch (method) {
            case "GET":
                getMovieById(exchange, id);
                break;

            case "DELETE":
                deleteMovie(exchange, id);
                break;

            default:
                sendError(
                        exchange,
                        405,
                        "Метод не поддерживается"
                );
        }
    }

    private void getMovieById(
            HttpExchange exchange,
            int id
    ) throws IOException {

        Movie movie =
                store.findById(id);

        if (movie == null) {

            sendError(
                    exchange,
                    404,
                    "Фильм не найден"
            );

            return;
        }

        sendJsonResponse(
                exchange,
                200,
                gson.toJson(movie)
        );
    }

    private void deleteMovie(
            HttpExchange exchange,
            int id
    ) throws IOException {

        boolean deleted =
                store.delete(id);

        if (!deleted) {

            sendError(
                    exchange,
                    404,
                    "Фильм не найден"
            );

            return;
        }

        sendNoContent(exchange);
    }

    private String readBody(
            HttpExchange exchange
    ) throws IOException {

        try (InputStream input =
                     exchange.getRequestBody()) {

            return new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }
    }

    private void sendValidationError(
            HttpExchange exchange,
            List<String> details
    ) throws IOException {

        ErrorResponse error =
                new ErrorResponse(
                        "Ошибка валидации",
                        details
                );

        sendJsonResponse(
                exchange,
                422,
                gson.toJson(error)
        );
    }

    private void sendError(
            HttpExchange exchange,
            int statusCode,
            String message
    ) throws IOException {

        ErrorResponse error =
                new ErrorResponse(message);

        sendJsonResponse(
                exchange,
                statusCode,
                gson.toJson(error)
        );
    }
}