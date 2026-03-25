package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private HttpServer server;
    private MoviesStore moviesStore;
    private int port;

    public MoviesServer(HttpServer server,MoviesStore moviesStore, int port){
        super();
    }
    public MoviesServer(MoviesStore store, int port) {
        try {
            // создайте сервер
            moviesStore = store;
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/movies", new MoviesHandlerMovies(moviesStore));
            server.start();
            System.out.println("Сервер запущен");
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер", e);
        }
    }

    public void stopp() {
        // остановите сервер
        server.stop(0);
        System.out.println("Сервер остановлен");
    }

    public void clear() {
        moviesStore.clearMovies();
    }
}