package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new HashMap<>();
    private int nextId = 1;

    public Movie add(String title, int year) {
        Movie movie = new Movie(nextId++, title, year);
        movies.put(movie.getId(), movie);
        return movie;
    }

    public Collection<Movie> findAll() {
        return movies.values();
    }

    public Movie findById(int id) {
        return movies.get(id);
    }

    public Movie delete(int id) {
        return movies.remove(id);
    }

    public void clear() {
        movies.clear();
        nextId = 1;
    }
}