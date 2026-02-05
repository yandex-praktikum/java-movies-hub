package ru.practicum.moviehub;

import handlersandserver.MoviesServer;

public class MovieHubApp {
    public static void main(String[] args) {
        final MoviesServer server = new MoviesServer();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
    }
}