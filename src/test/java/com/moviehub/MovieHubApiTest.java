package com.moviehub;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.moviehub.server.MovieHubServer;
import com.moviehub.storage.MovieStorage;
import com.moviehub.validation.MovieValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovieHubApiTest {

    private static final String JSON = "application/json; charset=UTF-8";

    private MovieHubServer server;
    private HttpClient client;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = new MovieHubServer(0, new MovieStorage());
        server.start();
        client = HttpClient.newHttpClient();
        baseUrl = "http://localhost:" + server.getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> delete(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path)).DELETE().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> post(String body, String contentType) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> postJson(String body) throws Exception {
        return post(body, JSON);
    }

    private HttpResponse<String> send(String method, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private JsonObject addMovie(String title, int year) throws Exception {
        HttpResponse<String> response = postJson("{\"title\":\"" + title + "\",\"year\":" + year + "}");
        assertEquals(201, response.statusCode());
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private JsonElement json(HttpResponse<String> response) {
        return JsonParser.parseString(response.body());
    }

    private void assertJsonContentType(HttpResponse<String> response) {
        assertEquals(JSON, response.headers().firstValue("Content-Type").orElse(null));
    }

    private void assertValidationError(HttpResponse<String> response) {
        assertEquals(422, response.statusCode());
        assertJsonContentType(response);
        JsonObject body = json(response).getAsJsonObject();
        assertEquals("Ошибка валидации", body.get("error").getAsString());
        assertTrue(body.get("details").isJsonArray());
        assertTrue(body.getAsJsonArray("details").size() > 0);
    }

    @Test
    void getMoviesReturnsEmptyListWhenNoMovies() throws Exception {
        HttpResponse<String> response = get("/movies");

        assertEquals(200, response.statusCode());
        assertJsonContentType(response);
        JsonArray body = json(response).getAsJsonArray();
        assertEquals(0, body.size());
    }

    @Test
    void getMoviesReturnsPreviouslyAddedMovies() throws Exception {
        addMovie("Матрица", 1999);
        addMovie("Начало", 2010);

        HttpResponse<String> response = get("/movies");

        assertEquals(200, response.statusCode());
        assertJsonContentType(response);
        JsonArray body = json(response).getAsJsonArray();
        assertEquals(2, body.size());
        assertEquals("Матрица", body.get(0).getAsJsonObject().get("title").getAsString());
        assertEquals("Начало", body.get(1).getAsJsonObject().get("title").getAsString());
    }

    @Test
    void postMovieCreatesMovieWithValidData() throws Exception {
        HttpResponse<String> response = postJson("{\"title\":\"Интерстеллар\",\"year\":2014}");

        assertEquals(201, response.statusCode());
        assertJsonContentType(response);
        JsonObject body = json(response).getAsJsonObject();
        assertEquals(1, body.get("id").getAsLong());
        assertEquals("Интерстеллар", body.get("title").getAsString());
        assertEquals(2014, body.get("year").getAsInt());
    }

    @Test
    void postMovieAssignsIncrementingIds() throws Exception {
        JsonObject first = addMovie("Первый", 2000);
        JsonObject second = addMovie("Второй", 2001);

        assertEquals(1, first.get("id").getAsLong());
        assertEquals(2, second.get("id").getAsLong());
    }

    @Test
    void postMovieAcceptsBoundaryYears() throws Exception {
        assertEquals(201, postJson("{\"title\":\"Старт\",\"year\":1888}").statusCode());
        assertEquals(201, postJson("{\"title\":\"Финиш\",\"year\":" + MovieValidator.maxYear() + "}").statusCode());
    }

    @Test
    void postMovieAcceptsTitleOfMaximumLength() throws Exception {
        String title = "a".repeat(100);

        assertEquals(201, postJson("{\"title\":\"" + title + "\",\"year\":2000}").statusCode());
    }

    @Test
    void postMovieRejectsEmptyTitle() throws Exception {
        HttpResponse<String> response = postJson("{\"title\":\"\",\"year\":2000}");

        assertValidationError(response);
    }

    @Test
    void postMovieRejectsBlankTitle() throws Exception {
        HttpResponse<String> response = postJson("{\"title\":\"   \",\"year\":2000}");

        assertValidationError(response);
    }

    @Test
    void postMovieRejectsMissingTitle() throws Exception {
        HttpResponse<String> response = postJson("{\"year\":2000}");

        assertValidationError(response);
    }

    @Test
    void postMovieRejectsTooLongTitle() throws Exception {
        String title = "a".repeat(101);

        HttpResponse<String> response = postJson("{\"title\":\"" + title + "\",\"year\":2000}");

        assertValidationError(response);
    }

    @Test
    void postMovieRejectsYearBelowMinimum() throws Exception {
        HttpResponse<String> response = postJson("{\"title\":\"Фильм\",\"year\":1887}");

        assertValidationError(response);
    }

    @Test
    void postMovieRejectsYearAboveMaximum() throws Exception {
        int year = MovieValidator.maxYear() + 1;

        HttpResponse<String> response = postJson("{\"title\":\"Фильм\",\"year\":" + year + "}");

        assertValidationError(response);
    }

    @Test
    void postMovieRejectsMissingYear() throws Exception {
        HttpResponse<String> response = postJson("{\"title\":\"Фильм\"}");

        assertValidationError(response);
    }

    @Test
    void postMovieRejectsNonNumericYear() throws Exception {
        HttpResponse<String> response = postJson("{\"title\":\"Фильм\",\"year\":\"abc\"}");

        assertValidationError(response);
    }

    @Test
    void postMovieRejectsFractionalYear() throws Exception {
        HttpResponse<String> response = postJson("{\"title\":\"Фильм\",\"year\":2000.5}");

        assertValidationError(response);
    }

    @Test
    void postMovieReturnsAllValidationDetails() throws Exception {
        HttpResponse<String> response = postJson("{\"title\":\"\",\"year\":1500}");

        assertEquals(422, response.statusCode());
        JsonObject body = json(response).getAsJsonObject();
        assertEquals(2, body.getAsJsonArray("details").size());
    }

    @Test
    void postMovieRejectsWrongContentType() throws Exception {
        HttpResponse<String> response = post("{\"title\":\"Фильм\",\"year\":2000}", "text/plain");

        assertEquals(415, response.statusCode());
        assertJsonContentType(response);
        assertTrue(json(response).getAsJsonObject().has("error"));
    }

    @Test
    void postMovieRejectsMissingContentType() throws Exception {
        HttpResponse<String> response = post("{\"title\":\"Фильм\",\"year\":2000}", null);

        assertEquals(415, response.statusCode());
    }

    @Test
    void postMovieRejectsInvalidJson() throws Exception {
        HttpResponse<String> response = postJson("{\"title\":\"Фильм\",");

        assertEquals(400, response.statusCode());
        assertJsonContentType(response);
        assertTrue(json(response).getAsJsonObject().has("error"));
    }

    @Test
    void postMovieRejectsNonObjectJson() throws Exception {
        assertEquals(400, postJson("[1,2,3]").statusCode());
        assertEquals(400, postJson("").statusCode());
    }

    @Test
    void postMovieDoesNotStoreInvalidMovie() throws Exception {
        postJson("{\"title\":\"\",\"year\":2000}");

        assertEquals(0, json(get("/movies")).getAsJsonArray().size());
    }

    @Test
    void getMovieByIdReturnsExistingMovie() throws Exception {
        addMovie("Матрица", 1999);

        HttpResponse<String> response = get("/movies/1");

        assertEquals(200, response.statusCode());
        assertJsonContentType(response);
        JsonObject body = json(response).getAsJsonObject();
        assertEquals(1, body.get("id").getAsLong());
        assertEquals("Матрица", body.get("title").getAsString());
        assertEquals(1999, body.get("year").getAsInt());
    }

    @Test
    void getMovieByIdReturnsNotFoundForUnknownId() throws Exception {
        HttpResponse<String> response = get("/movies/42");

        assertEquals(404, response.statusCode());
        assertJsonContentType(response);
        assertEquals("Фильм не найден", json(response).getAsJsonObject().get("error").getAsString());
    }

    @Test
    void getMovieByIdReturnsBadRequestForNonNumericId() throws Exception {
        HttpResponse<String> response = get("/movies/abc");

        assertEquals(400, response.statusCode());
        assertJsonContentType(response);
        assertEquals("Некорректный ID", json(response).getAsJsonObject().get("error").getAsString());
    }

    @Test
    void deleteMovieRemovesExistingMovie() throws Exception {
        addMovie("Матрица", 1999);

        HttpResponse<String> response = delete("/movies/1");

        assertEquals(204, response.statusCode());
        assertEquals("", response.body());
        assertEquals(404, get("/movies/1").statusCode());
        assertEquals(0, json(get("/movies")).getAsJsonArray().size());
    }

    @Test
    void deleteMovieReturnsNotFoundForUnknownId() throws Exception {
        HttpResponse<String> response = delete("/movies/42");

        assertEquals(404, response.statusCode());
        assertJsonContentType(response);
        assertTrue(json(response).getAsJsonObject().has("error"));
    }

    @Test
    void deleteMovieReturnsBadRequestForNonNumericId() throws Exception {
        HttpResponse<String> response = delete("/movies/abc");

        assertEquals(400, response.statusCode());
        assertJsonContentType(response);
        assertEquals("Некорректный ID", json(response).getAsJsonObject().get("error").getAsString());
    }

    @Test
    void deleteMovieTwiceReturnsNotFoundSecondTime() throws Exception {
        addMovie("Матрица", 1999);

        assertEquals(204, delete("/movies/1").statusCode());
        assertEquals(404, delete("/movies/1").statusCode());
    }

    @Test
    void filterByYearReturnsMatchingMovies() throws Exception {
        addMovie("Матрица", 1999);
        addMovie("Бойцовский клуб", 1999);
        addMovie("Начало", 2010);

        HttpResponse<String> response = get("/movies?year=1999");

        assertEquals(200, response.statusCode());
        assertJsonContentType(response);
        JsonArray body = json(response).getAsJsonArray();
        assertEquals(2, body.size());
        for (JsonElement element : body) {
            assertEquals(1999, element.getAsJsonObject().get("year").getAsInt());
        }
    }

    @Test
    void filterByYearReturnsEmptyListWhenNothingMatches() throws Exception {
        addMovie("Матрица", 1999);

        HttpResponse<String> response = get("/movies?year=2005");

        assertEquals(200, response.statusCode());
        assertEquals(0, json(response).getAsJsonArray().size());
    }

    @Test
    void filterByYearReturnsBadRequestForNonNumericYear() throws Exception {
        HttpResponse<String> response = get("/movies?year=abc");

        assertEquals(400, response.statusCode());
        assertJsonContentType(response);
        assertEquals("Некорректный параметр запроса — 'year'",
                json(response).getAsJsonObject().get("error").getAsString());
    }

    @Test
    void filterByYearReturnsBadRequestForEmptyYear() throws Exception {
        assertEquals(400, get("/movies?year=").statusCode());
        assertEquals(400, get("/movies?year").statusCode());
    }

    @Test
    void unsupportedMethodOnCollectionReturns405() throws Exception {
        for (String method : new String[]{"PUT", "DELETE", "PATCH"}) {
            HttpResponse<String> response = send(method, "/movies");

            assertEquals(405, response.statusCode(), method);
            assertJsonContentType(response);
            assertTrue(json(response).getAsJsonObject().has("error"));
        }
    }

    @Test
    void unsupportedMethodOnItemReturns405() throws Exception {
        for (String method : new String[]{"PUT", "POST", "PATCH"}) {
            HttpResponse<String> response = send(method, "/movies/1");

            assertEquals(405, response.statusCode(), method);
            assertJsonContentType(response);
            assertTrue(json(response).getAsJsonObject().has("error"));
        }
    }

    @Test
    void unknownPathReturnsNotFound() throws Exception {
        HttpResponse<String> response = get("/unknown");

        assertEquals(404, response.statusCode());
        assertJsonContentType(response);
        assertTrue(json(response).getAsJsonObject().has("error"));
    }
}
