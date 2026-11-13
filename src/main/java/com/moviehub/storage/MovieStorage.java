package com.moviehub.storage;

import com.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MovieStorage {

    private final Map<Long, Movie> movies = new LinkedHashMap<>();
    private long nextId = 1;

    public synchronized Movie add(String title, int year) {
        Movie movie = new Movie(nextId++, title, year);
        movies.put(movie.getId(), movie);
        return movie;
    }

    public synchronized List<Movie> findAll() {
        return new ArrayList<>(movies.values());
    }

    public synchronized List<Movie> findByYear(int year) {
        List<Movie> result = new ArrayList<>();
        for (Movie movie : movies.values()) {
            if (movie.getYear() == year) {
                result.add(movie);
            }
        }
        return result;
    }

    public synchronized Optional<Movie> findById(long id) {
        return Optional.ofNullable(movies.get(id));
    }

    public synchronized boolean delete(long id) {
        return movies.remove(id) != null;
    }
}
