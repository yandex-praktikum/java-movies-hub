package ru.practicum.moviehub.api;

import java.util.Arrays;

public class ErrorResponse {
    private String error;
    private String[] detail;

    public ErrorResponse(String error, String[] detail) {
        this.error = error;
        this.detail = detail;
    }

    @Override
    public String toString() {
        return error + " - " + Arrays.toString(detail);
    }
}