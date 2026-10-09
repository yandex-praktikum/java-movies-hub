package ru.practicum.moviehub.http;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Year;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {

    private static final String BASE_URL = "http://localhost:8080";

    private static MoviesServer server;
    private static MoviesStore moviesStore;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() {
        moviesStore = new MoviesStore();
        server = new MoviesServer(moviesStore, 8080);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        moviesStore.clear();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpResponse<String> response = get("/movies");

        assertEquals(200, response.statusCode());
        assertEquals(
                "application/json; charset=UTF-8",
                response.headers().firstValue("Content-Type").orElse("")
        );
        assertEquals("[]", response.body());
    }

    @Test
    void getMovies_whenMoviesExist_returnsArray() throws Exception {
        moviesStore.add("Интерстеллар", 2014);

        HttpResponse<String> response = get("/movies");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().startsWith("["));
        assertTrue(response.body().contains("\"title\":\"Интерстеллар\""));
    }

    @Test
    void addMovie_whenValid_returnsCreatedMovie() throws Exception {
        HttpResponse<String> response = post(
                "{\"title\":\"Интерстеллар\",\"year\":2014}"
        );

        assertEquals(201, response.statusCode());
        assertTrue(response.body().contains("\"id\":"));
        assertTrue(response.body().contains("\"title\":\"Интерстеллар\""));
        assertTrue(response.body().contains("\"year\":2014"));
        assertTrue(response.headers().firstValue("Content-Type")
                .orElse("").contains("application/json"));
    }

    @Test
    void addMovie_whenTitleEmpty_returns422() throws Exception {
        HttpResponse<String> response = post(
                "{\"title\":\"\",\"year\":2014}"
        );

        assertEquals(422, response.statusCode());
        assertTrue(response.body().contains("\"error\""));
        assertTrue(response.body().contains("\"details\""));
    }

    @Test
    void addMovie_whenTitleTooLong_returns422() throws Exception {
        String title = "A".repeat(101);
        HttpResponse<String> response = post(
                "{\"title\":\"" + title + "\",\"year\":2014}"
        );

        assertEquals(422, response.statusCode());
    }

    @Test
    void addMovie_whenYearTooEarly_returns422() throws Exception {
        HttpResponse<String> response = post(
                "{\"title\":\"Фильм\",\"year\":1800}"
        );

        assertEquals(422, response.statusCode());
    }

    @Test
    void addMovie_whenYearTooLate_returns422() throws Exception {
        int invalidYear = Year.now().getValue() + 2;

        HttpResponse<String> response = post(
                "{\"title\":\"Фильм\",\"year\":" + invalidYear + "}"
        );

        assertEquals(422, response.statusCode());
    }

    @Test
    void addMovie_whenYearMissing_returns422() throws Exception {
        HttpResponse<String> response = post(
                "{\"title\":\"Интерстеллар\"}"
        );

        assertEquals(422, response.statusCode());
        assertTrue(response.body().contains("\"details\""));
    }

    @Test
    void addMovie_whenContentTypeUnsupported_returns415() throws Exception {
        HttpResponse<String> response = post(
                "{\"title\":\"Фильм\",\"year\":2014}",
                "text/plain"
        );

        assertEquals(415, response.statusCode());
    }

    @Test
    void addMovie_whenJsonInvalid_returns400() throws Exception {
        HttpResponse<String> response = post(
                "{\"title\":\"Фильм\",\"year\":"
        );

        assertEquals(400, response.statusCode());
    }

    @Test
    void getMovieById_whenExists_returnsMovie() throws Exception {
        Movie movie = moviesStore.add("Интерстеллар", 2014);

        HttpResponse<String> response = get("/movies/" + movie.getId());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"title\":\"Интерстеллар\""));
        assertTrue(response.body().contains("\"year\":2014"));
    }

    @Test
    void getMovieById_whenNotFound_returns404() throws Exception {
        HttpResponse<String> response = get("/movies/999");

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("Фильм не найден"));
    }

    @Test
    void getMovieById_whenIdNotNumeric_returns400() throws Exception {
        HttpResponse<String> response = get("/movies/abc");

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Некорректный ID"));
    }

    @Test
    void deleteMovie_whenExists_returns204() throws Exception {
        Movie movie = moviesStore.add("Интерстеллар", 2014);

        HttpResponse<String> response = delete("/movies/" + movie.getId());

        assertEquals(204, response.statusCode());
        assertEquals("", response.body());
    }

    @Test
    void deleteMovie_whenNotFound_returns404() throws Exception {
        HttpResponse<String> response = delete("/movies/999");

        assertEquals(404, response.statusCode());
    }

    @Test
    void deleteMovie_whenIdNotNumeric_returns400() throws Exception {
        HttpResponse<String> response = delete("/movies/abc");

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Некорректный ID"));
    }

    @Test
    void getMovies_whenYearProvided_filtersMovies() throws Exception {
        moviesStore.add("Интерстеллар", 2014);
        moviesStore.add("Матрица", 1999);

        HttpResponse<String> response = get("/movies?year=2014");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"title\":\"Интерстеллар\""));
        assertFalse(response.body().contains("\"title\":\"Матрица\""));
    }

    @Test
    void getMovies_whenYearInvalid_returns400() throws Exception {
        HttpResponse<String> response = get("/movies?year=abc");

        assertEquals(400, response.statusCode());
    }

    @Test
    void request_whenMethodUnsupported_returns405() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(405, response.statusCode());
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .GET()
                .build();

        return client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }

    private HttpResponse<String> post(String body) throws Exception {
        return post(body, "application/json");
    }

    private HttpResponse<String> post(
            String body,
            String contentType
    ) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(
                        body, StandardCharsets.UTF_8))
                .build();

        return client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }

    private HttpResponse<String> delete(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .DELETE()
                .build();

        return client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }
}