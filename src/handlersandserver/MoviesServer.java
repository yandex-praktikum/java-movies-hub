package handlersandserver;

import com.sun.net.httpserver.HttpServer;
import workingwithmovies.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private final HttpServer server;

    public MoviesServer() {
        try {
            MoviesStore store = new MoviesStore();
            MoviesHandler moviesHandler = new MoviesHandler(store);
            server = HttpServer.create(new InetSocketAddress(8081), 0);

            // Добавьте контекст для /movies и укажите созданный хендлер
            server.createContext("/movies", moviesHandler);

        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер", e);
        }
    }

    public void start() {
        server.start();
        System.out.println("Сервер запущен");
    }

    public void stop() {
        server.stop(0);
        System.out.println("Сервер остановлен");
    }
}
