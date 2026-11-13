package com.moviehub.model;

import java.util.List;

public class ErrorResponse {

    private final String error;
    private final List<String> details;

    public ErrorResponse(String error) {
        this(error, null);
    }

    public ErrorResponse(String error, List<String> details) {
        this.error = error;
        this.details = details;
    }

    public String getError() {
        return error;
    }

    public List<String> getDetails() {
        return details;
    }
}
