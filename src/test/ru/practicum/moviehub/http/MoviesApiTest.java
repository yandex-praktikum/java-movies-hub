package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import handlersandserver.MoviesServer;
import org.junit.jupiter.api.*;
import workingwithmovies.Movie;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8081";
    private static MoviesServer server;
    private static HttpClient client;
    private static Gson gson;

    @BeforeEach
    void beforeEach() {
        gson = new Gson();
        server = new MoviesServer();
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @AfterEach
    void afterEach() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void post_Movie_return_OK() throws Exception {
        Movie movie = new Movie(1, "Новый фильм", 120);
        String jsonBody = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        String body = resp.body().trim();
        assertEquals("\"Фильм успешно добавлен в список\"", body);
    }

    @Test
    void post_Exists_Movie_returnError() throws IOException, InterruptedException {
        Movie movie = new Movie(1, "Новый фильм", 120);
        String jsonBody = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String body = resp.body().trim();
        assertEquals("Фильм уже есть в списке", body);
    }

    @Test
    void getMovie_by_id() throws IOException, InterruptedException {
        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        Movie movie = new Movie(1, "Новый фильм", 120);
        String json = gson.toJson(movie);

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        String body = resp.body().trim();
        assertEquals("\"Фильм успешно добавлен в список\"", body);


        resp = client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        assertEquals(json, resp.body());

        HttpRequest getReqNotExists = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/2"))
                .GET()
                .build();
        resp = client.send(getReqNotExists, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals("Такого фильма нет в списке", resp.body());
    }

    @Test
    void deleteMovie_by_id() throws IOException, InterruptedException {
        Movie movie = new Movie(1, "Новый фильм", 120);
        String json = gson.toJson(movie);

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();


        HttpResponse<String> resp =
                client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        String body = resp.body().trim();
        assertEquals("\"Фильм успешно добавлен в список\"", body);

        HttpRequest deleteReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .DELETE()
                .build();

        resp = client.send(deleteReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals("Фильм успешно удалён", resp.body());
    }

    @Test
    void returnAllMovies() throws IOException, InterruptedException {
        Movie movie1 = new Movie(1, "Первый фильм", 120);
        Movie movie2 = new Movie(2, "Второй фильм", 100);

        HttpRequest reqMovie1 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie1)))
                .build();

        HttpRequest reqMovie2 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie2)))
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> respMove1 =
                client.send(reqMovie1, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpResponse<String> respMove2 =
                client.send(reqMovie2, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        String jsonResp = resp.body();
        String jsonMovies = "[" + gson.toJson(movie1) + "," + gson.toJson(movie2) + "]";
        assertEquals(jsonMovies, jsonResp);
    }
}