package ru.practicum.moviehub.model;

public class Movie {
    private int id;
    private String title;
    private int year;

    public Movie(int id, String title, int year) {
        this.id = id;
        this.title = title;
        this.year = year;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getYear() {
        return year;
    }
}
