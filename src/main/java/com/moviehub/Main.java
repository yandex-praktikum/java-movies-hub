package com.moviehub;

import com.moviehub.server.MovieHubServer;
import com.moviehub.storage.MovieStorage;

import java.io.IOException;

public class Main {

    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        MovieHubServer server = new MovieHubServer(PORT, new MovieStorage());
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
        System.out.println("MovieHub запущен: http://localhost:" + server.getPort());
    }
}
