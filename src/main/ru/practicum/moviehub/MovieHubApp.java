package ru.practicum.moviehub;


import ru.practicum.moviehub.http.MoviesServer;
import ru.practicum.moviehub.store.MoviesStore;

public class MovieHubApp {
    public static void main(String[] args) {
        try {
            MoviesServer serv = new MoviesServer(new MoviesStore(), 8080);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutdown hook triggered...");
                serv.stopp();
            }));
            //serv.stat();
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}