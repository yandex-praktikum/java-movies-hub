package ru.practicum.moviehub;

import ru.practicum.moviehub.http.MoviesServer;
import ru.practicum.moviehub.store.MoviesStore;

public class MovieHubApp {
    public static void main(String[] args) {
        final MoviesServer server = new MoviesServer(new MoviesStore(), 8080);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
    }
}