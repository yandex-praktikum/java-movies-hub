package workingwithmovies;

import errors.IdAlreadyExistsException;
import errors.MovieAlreadyExistsException;
import errors.MovieNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MoviesStore {
    private final List<Movie> movies = new ArrayList<>();

    public List<Movie> getAllMovies() {
        System.out.println("Возвращаем список: " + movies);
        return movies;
    }

    public void addMovies(Movie movie) {
        boolean titleExists = movies.stream()
                .anyMatch(movie1 -> movie1.getTitle().equals(movie.getTitle()));
        if (titleExists) {
            throw new MovieAlreadyExistsException("Фильм с таким именем уже есть в списке");
        }

        Optional<Movie> addMovie = movies.stream()
                .filter(movie1 -> movie1.getId() == movie.getId())
                .findFirst();
        if (addMovie.isPresent()) {
            throw new IdAlreadyExistsException("Такой id уже занят");
        }

        movies.add(movie);
        System.out.println("Добавляем фильм: " + movie.getTitle());
    }

    public Movie findMovie(int id) {
        Optional<Movie> foundMovie = movies.stream()
                .filter(movie -> movie.getId() == id)
                .findFirst();

        if (foundMovie.isEmpty()) {
            throw new MovieNotFoundException("Фильма с таким id: {" + id + "} нет в списке");
        }
        return foundMovie.get();
    }

    public void deleteMovieById(int id) {
        Optional<Movie> deleteMovie = movies.stream()
                .filter(movie -> movie.getId() == id)
                .findFirst();

        if (deleteMovie.isEmpty()) {
            throw new MovieNotFoundException("Фильма с таким id: {" + id + "} нет в списке");
        }
        movies.remove(deleteMovie.get());
    }
}
