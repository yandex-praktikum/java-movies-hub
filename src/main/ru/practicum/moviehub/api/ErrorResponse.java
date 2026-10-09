package ru.practicum.moviehub.api;

import java.util.List;

public class ErrorResponse {
    private final String error;
    private final List<String> details;

    public ErrorResponse(String error, List<String> details) {
        this.error = error;
        this.details = details;
    }
}