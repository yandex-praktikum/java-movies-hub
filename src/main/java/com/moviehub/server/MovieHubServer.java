package com.moviehub.server;

import com.moviehub.storage.MovieStorage;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class MovieHubServer {

    private final HttpServer server;

    public MovieHubServer(int port, MovieStorage storage) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/", new MoviesHandler(storage));
        this.server.setExecutor(Executors.newFixedThreadPool(8));
    }

    public void start() {
        server.start();
    }

    public void stop(int delaySeconds) {
        server.stop(delaySeconds);
    }

    public int getPort() {
        return server.getAddress().getPort();
    }
}
