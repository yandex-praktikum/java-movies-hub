package handlersandserver;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import errors.ErrorResponse;
import errors.IdAlreadyExistsException;
import errors.MovieAlreadyExistsException;
import errors.MovieNotFoundException;
import workingwithmovies.Movie;
import workingwithmovies.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore moviesStore;
    private final Gson gson = new Gson();

    public MoviesHandler(MoviesStore store) {
        this.moviesStore = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {

        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().toString();
        System.out.println("Текущий путь: " + path);
        try {
            if (method.equalsIgnoreCase("GET")) {
                if (path.equals("/movies")) {
                    sendJson(ex, 200, gson.toJson(moviesStore.getAllMovies()));
                } else if (path.matches("/movies/\\d+")) {
                    int id = Integer.parseInt(path.split("/")[2]);
                    System.out.println("title: " + id);
                    sendJson(ex, 200, gson.toJson(moviesStore.findMovie(id)));
                } else {
                    sendJson(ex, 404, new ErrorResponse(404, "Неизвестный эндпоинт").getMessage());
                }
            } else if (method.equalsIgnoreCase("POST")) {
                if (path.equals("/movies")) {
                    String requestBody = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    Movie movie = gson.fromJson(requestBody, Movie.class);
                    System.out.println(movie);
                    moviesStore.addMovies(movie);
                    sendJson(ex, 201, gson.toJson("Фильм успешно добавлен в список"));
                } else {
                    sendJson(ex, 405, new ErrorResponse(405, "Метод не разрешён для этого пути").getMessage());
                }
            } else if (method.equalsIgnoreCase("DELETE")) {
                if (path.matches("/movies/\\d+")) {
                    int id = Integer.parseInt(path.split("/")[2]);
                    moviesStore.deleteMovieById(id);
                    sendJson(ex, 201, "Фильм успешно удалён");
                } else {
                    sendJson(ex, 405, new ErrorResponse(405, "Метод не разрешён для этого пути").getMessage());
                }
            } else {
                sendNoContent(ex);
            }
        } catch (IdAlreadyExistsException e) {
            sendJson(ex, 409, new ErrorResponse(409, "Этот id занят").getMessage());
        } catch (MovieNotFoundException e) {
            sendJson(ex, 404, new ErrorResponse(404, "Такого фильма нет в списке").getMessage());
        } catch (MovieAlreadyExistsException e) {
            sendJson(ex, 409, new ErrorResponse(409, "Фильм уже есть в списке").getMessage());
        } catch (Exception e) {
            sendJson(ex, 500, new ErrorResponse(500, e.getMessage()).getMessage());
        }
    }
}