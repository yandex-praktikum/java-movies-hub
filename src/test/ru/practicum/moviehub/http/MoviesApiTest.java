package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    static MoviesServer server;
    static HttpClient client;
    static final String BASE = "http://localhost:8080";
    static Gson gson = new GsonBuilder()
            .create();

    /*@BeforeAll
    static void beforeAll() {
        server = new MoviesServer(new MoviesStore(), 8080);
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        //server.stat();
    }*/

    @BeforeEach
    void beforeEach() {
        server.clear();
    }

    @AfterAll
    static void afterAll() {
        server.stopp();
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
    void getMovies_returnsArrayMovies() throws Exception {
        List<Movie> movies = new ArrayList<>();
        movies.add(new Movie(1, "Вот такие фа", 2000));
        movies.add(new Movie(2, "Пчел", 2002));

        String jsonData1 = gson.toJson(movies.getFirst());
        String jsonData2 = gson.toJson(movies.getLast());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonData1))
                .build();
        client.send(request, HttpResponse.BodyHandlers.ofString());

        request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonData2))
                .build();
        client.send(request, HttpResponse.BodyHandlers.ofString());

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
        List<Movie> moviesList = gson.fromJson(body, new ListOfMoviesTypeToken().getType());
        assertEquals(movies, moviesList,
                "Ожидается JSON-массив");
    }

    @Test
    void postMovies_returnsMovie() throws Exception {
        Movie movie = new Movie(1, "Вот такие фа", 2000);
        String jsonData = gson.toJson(movie);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        Movie movieResp = gson.fromJson(body, Movie.class);
        assertEquals(movie, movieResp,
                "Ожидается JSON-массив");
    }

    @Test
    void postMovies_returnsErrorTitleNull() throws Exception {
        Movie movie = new Movie(1, "", 2000);
        String jsonData = gson.toJson(movie);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String body = resp.body().trim();
        assertTrue(body.contains("error"));
    }

    @Test
    void postMovies_returnsErrorTitleLengthExpection() throws Exception {
        Movie movie = new Movie(1, "-".repeat(1000), 2000);
        String jsonData = gson.toJson(movie);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String body = resp.body().trim();
        assertTrue(body.contains("error"));
    }

    @Test
    void postMovies_returnsErrorYear() throws Exception {
        Movie movie = new Movie(1, "title", 1887);
        String jsonData = gson.toJson(movie);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        movie = new Movie(1, "title", 2027);
        jsonData = gson.toJson(movie);

        request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .build();

        resp = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String body = resp.body().trim();
        assertTrue(body.contains("error"));
    }

    @Test
    void postMovies_returnsErrorJson() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString("фа"))
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(415, resp.statusCode(), "POST /movies должен вернуть 415");

        String body = resp.body().trim();
        assertTrue(body.contains("error"));
    }

    @Test
    void getMovies_returnsMovie() throws Exception {

        Movie movie = new Movie(1, "Вот такие фа", 2000);

        String jsonData1 = gson.toJson(movie);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonData1))
                .build();
        client.send(request, HttpResponse.BodyHandlers.ofString());

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
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
        Movie movieRes = gson.fromJson(body, Movie.class);
        assertEquals(movie, movieRes,
                "Ожидается JSON");
    }

    @Test
    void getMovies_returnsErrorNotFoundMovie() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(404, resp.statusCode(), "GET /movies должен вернуть 404");

        String body = resp.body().trim();
        assertTrue(body.contains("error"));
    }

    @Test
    void getMovies_returnsErrorIdIsNotNumber() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/asdasd"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, resp.statusCode(), "GET /movies должен вернуть 400");

        String body = resp.body().trim();
        assertTrue(body.contains("error"));
    }

    @Test
    void deleteMovies_returnsMovie() throws Exception {

        Movie movie = new Movie(1, "Вот такие фа", 2000);

        String jsonData1 = gson.toJson(movie);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonData1))
                .build();
        client.send(request, HttpResponse.BodyHandlers.ofString());

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(204, resp.statusCode(), "DELETE /movies должен вернуть 204");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
    }

    @Test
    void deleteMovies_returnsErrorNotFoundMovie() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(404, resp.statusCode(), "DELETE /movies должен вернуть 404");

        String body = resp.body().trim();
        assertTrue(body.contains("error"));
    }

    @Test
    void deleteMovies_returnsErrorIdIsNotNumber() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/asdasd"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, resp.statusCode(), "DELETE /movies должен вернуть 400");

        String body = resp.body().trim();
        assertTrue(body.contains("error"));
    }

    @Test
    void getMovies_returnsMovieByYear() throws Exception {

        List<Movie> movies = new ArrayList<>();
        movies.add(new Movie(1, "Вот такие фа", 2000));

        String jsonData1 = gson.toJson(movies.getFirst());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonData1))
                .build();
        client.send(request, HttpResponse.BodyHandlers.ofString());

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2000"))
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
        List<Movie> moviesList = gson.fromJson(body, new ListOfMoviesTypeToken().getType());
        assertEquals(movies, moviesList,
                "Ожидается JSON-список");
    }

    @Test
    void getMovies_returnsMovieByYearNull() throws Exception {

        List<Movie> movies = new ArrayList<>();
        movies.add(new Movie(1, "Вот такие фа", 2000));

        String jsonData1 = gson.toJson(movies.getFirst());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonData1))
                .build();
        client.send(request, HttpResponse.BodyHandlers.ofString());

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2020"))
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
        List<Movie> moviesList = gson.fromJson(body, new ListOfMoviesTypeToken().getType());
        List<Movie> moviesNull = new ArrayList<>();
        assertEquals(moviesNull, moviesList,
                "Ожидается JSON-список");
    }

    @Test
    void getMovies_returnsMovieByYearErrorYearNotNumber() throws Exception {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=sasa"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, resp.statusCode(), "GET /movies должен вернуть 400");

        String body = resp.body().trim();
        assertTrue(body.contains("error"));
    }
}