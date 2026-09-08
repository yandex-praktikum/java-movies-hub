package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoviesStore {

    private final Map<Integer, Movie> movies = new HashMap<>();
    private int nextId = 1;

    public List<Movie> findAll() {
        return new ArrayList<>(movies.values());
    }

    public Movie findById(int id) {
        return movies.get(id);
    }

    public Movie add(String title, int year) {
        Movie movie = new Movie(
                nextId++,
                title,
                year
        );

        movies.put(
                movie.getId(),
                movie
        );

        return movie;
    }

    public boolean delete(int id) {
        return movies.remove(id) != null;
    }

    public List<Movie> findByYear(int year) {
        List<Movie> result = new ArrayList<>();

        for (Movie movie : movies.values()) {
            if (movie.getYear() == year) {
                result.add(movie);
            }
        }

        return result;
    }

    public void clear() {
        movies.clear();
        nextId = 1;
    }
}