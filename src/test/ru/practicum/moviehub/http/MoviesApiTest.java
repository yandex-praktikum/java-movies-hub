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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {

    private static MoviesServer server;
    private static MoviesStore store;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();

        server = new MoviesServer(
                store,
                8080
        );

        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray()
            throws Exception {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/movies"
                ))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        assertEquals(200, resp.statusCode());

        assertEquals(
                "application/json; charset=UTF-8",
                resp.headers()
                        .firstValue("Content-Type")
                        .orElse("")
        );

        assertEquals(
                "[]",
                resp.body().trim()
        );
    }

    @Test
    void getMovies_whenMoviesExist_returnsMovies()
            throws Exception {

        store.add(
                "Матрица",
                1999
        );

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/movies"
                ))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        assertEquals(200, resp.statusCode());

        assertTrue(
                resp.body().contains("Матрица")
        );

        assertTrue(
                resp.body().contains("1999")
        );
    }

    @Test
    void addMovie_withValidData_returnsCreatedMovie()
            throws Exception {

        String json = """
                {
                    "title": "Матрица",
                    "year": 1999
                }
                """;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/movies"
                ))
                .header(
                        "Content-Type",
                        "application/json"
                )
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                json,
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        assertEquals(201, resp.statusCode());

        assertEquals(
                "application/json; charset=UTF-8",
                resp.headers()
                        .firstValue("Content-Type")
                        .orElse("")
        );

        assertTrue(
                resp.body().contains("Матрица")
        );

        assertTrue(
                resp.body().contains("1999")
        );

        assertTrue(
                resp.body().contains("id")
        );
    }

    @Test
    void addMovie_withEmptyTitle_returnsUnprocessableEntity()
            throws Exception {

        String json = """
                {
                    "title": "",
                    "year": 1999
                }
                """;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/movies"
                ))
                .header(
                        "Content-Type",
                        "application/json"
                )
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                json,
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        assertEquals(422, resp.statusCode());

        assertTrue(
                resp.body().contains("error")
        );

        assertTrue(
                resp.body().contains("details")
        );
    }

    @Test
    void addMovie_withBlankTitle_returnsUnprocessableEntity()
            throws Exception {

        String json = """
                {
                    "title": "   ",
                    "year": 1999
                }
                """;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/movies"
                ))
                .header(
                        "Content-Type",
                        "application/json"
                )
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                json,
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        assertEquals(422, resp.statusCode());

        assertTrue(
                resp.body().contains("error")
        );

        assertTrue(
                resp.body().contains("details")
        );
    }

    @Test
    void addMovie_withLongTitle_returnsUnprocessableEntity()
            throws Exception {

        String title = "А".repeat(101);

        String json = """
                {
                    "title": "%s",
                    "year": 1999
                }
                """.formatted(title);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/movies"
                ))
                .header(
                        "Content-Type",
                        "application/json"
                )
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                json,
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        assertEquals(422, resp.statusCode());

        assertTrue(
                resp.body().contains("error")
        );

        assertTrue(
                resp.body().contains("details")
        );
    }

    @Test
    void addMovie_withYearBefore1888_returnsUnprocessableEntity()
            throws Exception {

        String json = """
                {
                    "title": "Матрица",
                    "year": 1887
                }
                """;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/movies"
                ))
                .header(
                        "Content-Type",
                        "application/json"
                )
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                json,
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        assertEquals(422, resp.statusCode());

        assertTrue(
                resp.body().contains("error")
        );

        assertTrue(
                resp.body().contains("details")
        );
    }

    @Test
    void addMovie_withFutureYear_returnsUnprocessableEntity()
            throws Exception {

        int futureYear =
                Year.now().getValue() + 2;

        String json = """
                {
                    "title": "Матрица",
                    "year": %d
                }
                """.formatted(futureYear);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/movies"
                ))
                .header(
                        "Content-Type",
                        "application/json"
                )
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                json,
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        assertEquals(422, resp.statusCode());

        assertTrue(
                resp.body().contains("error")
        );

        assertTrue(
                resp.body().contains("details")
        );
    }

    @Test
    void addMovie_withYear1888_returnsCreated()
            throws Exception {

        String json = """
                {
                    "title": "Первый фильм",
                    "year": 1888
                }
                """;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/movies"
                ))
                .header(
                        "Content-Type",
                        "application/json"
                )
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                json,
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        assertEquals(201, resp.statusCode());
    }

    @Test
    void addMovie_withWrongContentType_returnsUnsupportedMediaType()
            throws Exception {

        String json = """
                {
                    "title": "Матрица",
                    "year": 1999
                }
                """;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/movies"
                ))
                .header(
                        "Content-Type",
                        "text/plain"
                )
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                json,
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        assertEquals(415, resp.statusCode());

        assertTrue(
                resp.body().contains("error")
        );
    }

    @Test
    void addMovie_withoutContentType_returnsUnsupportedMediaType()
            throws Exception {

        String json = """
                {
                    "title": "Матрица",
                    "year": 1999
                }
                """;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/movies"
                ))
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                json,
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        assertEquals(415, resp.statusCode());

        assertTrue(
                resp.body().contains("error")
        );
    }

    @Test
    void addMovie_withInvalidJson_returnsUnprocessableEntity()
            throws Exception {

        String json = """
                {
                    "title": "Матрица",
                    "year":
                """;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/movies"
                ))
                .header(
                        "Content-Type",
                        "application/json"
                )
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                json,
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        assertEquals(422, resp.statusCode());

        assertTrue(
                resp.body().contains("error")
        );
    }

    @Test
    void getMovieById_whenMovieExists_returnsMovie()
            throws Exception {

        Movie movie =
                store.add(
                        "Матрица",
                        1999
                );

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/movies/"
                                + movie.getId()
                ))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        assertEquals(200, resp.statusCode());

        assertTrue(
                resp.body().contains("Матрица")
        );

        assertTrue(
                resp.body().contains("1999")
        );
    }

    @Test
    void getMovieById_whenMovieNotFound_returnsNotFound()
            throws Exception {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/movies/999999"
                ))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        assertEquals(404, resp.statusCode());

        assertTrue(
                resp.body().contains("Фильм не найден")
        );

        assertTrue(
                resp.body().contains("error")
        );
    }

    @Test
    void getMovieById_withInvalidId_returnsBadRequest()
            throws Exception {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/movies/abc"
                ))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        assertEquals(400, resp.statusCode());

        assertTrue(
                resp.body().contains("Некорректный ID")
        );

        assertTrue(
                resp.body().contains("error")
        );
    }

    @Test
    void deleteMovie_whenMovieExists_returnsNoContent()
            throws Exception {

        Movie movie =
                store.add(
                        "Матрица",
                        1999
                );

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/movies/"
                                + movie.getId()
                ))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        assertEquals(204, resp.statusCode());

        assertTrue(
                store.findById(movie.getId()) == null
        );
    }

    @Test
    void deleteMovie_whenMovieNotFound_returnsNotFound()
            throws Exception {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/movies/999999"
                ))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        assertEquals(404, resp.statusCode());

        assertTrue(
                resp.body().contains("Фильм не найден")
        );
    }

    @Test
    void deleteMovie_withInvalidId_returnsBadRequest()
            throws Exception {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/movies/abc"
                ))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        assertEquals(400, resp.statusCode());

        assertTrue(
                resp.body().contains("Некорректный ID")
        );
    }

    @Test
    void getMovies_withYear_returnsMoviesOfThatYear()
            throws Exception {

        store.add(
                "Матрица",
                1999
        );

        store.add(
                "Матрица 2",
                2003
        );

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/movies?year=1999"
                ))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        assertEquals(200, resp.statusCode());

        assertTrue(
                resp.body().contains("Матрица")
        );

        assertTrue(
                resp.body().contains("1999")
        );

        assertTrue(
                !resp.body().contains("2003")
        );
    }

    @Test
    void getMovies_withUnknownYear_returnsEmptyArray()
            throws Exception {

        store.add(
                "Матрица",
                1999
        );

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/movies?year=2020"
                ))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        assertEquals(200, resp.statusCode());

        assertEquals(
                "[]",
                resp.body().trim()
        );
    }

    @Test
    void getMovies_withInvalidYear_returnsBadRequest()
            throws Exception {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/movies?year=abc"
                ))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        assertEquals(
                400,
                resp.statusCode()
        );

        assertTrue(
                resp.body().contains("year"),
                "Ответ сервера: " + resp.body()
        );
    }

    @Test
    void unsupportedMethod_returnsMethodNotAllowed()
            throws Exception {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/movies"
                ))
                .method(
                        "PUT",
                        HttpRequest.BodyPublishers.noBody()
                )
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        assertEquals(
                405,
                resp.statusCode()
        );

        assertTrue(
                resp.body().contains("error")
        );
    }
}