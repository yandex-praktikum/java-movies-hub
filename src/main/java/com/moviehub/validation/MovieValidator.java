package com.moviehub.validation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public final class MovieValidator {

    public static final int MIN_YEAR = 1888;
    public static final int MAX_TITLE_LENGTH = 100;

    private MovieValidator() {
    }

    public static int maxYear() {
        return Year.now().getValue() + 1;
    }

    public static List<String> validate(JsonObject body) {
        List<String> errors = new ArrayList<>();
        validateTitle(body.get("title"), errors);
        validateYear(body.get("year"), errors);
        return errors;
    }

    private static void validateTitle(JsonElement element, List<String> errors) {
        if (element == null || element.isJsonNull()) {
            errors.add("название не должно быть пустым");
            return;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            errors.add("название должно быть строкой");
            return;
        }
        String title = element.getAsString();
        if (title.trim().isEmpty()) {
            errors.add("название не должно быть пустым");
        } else if (title.length() > MAX_TITLE_LENGTH) {
            errors.add("название не должно быть длиннее " + MAX_TITLE_LENGTH + " символов");
        }
    }

    private static void validateYear(JsonElement element, List<String> errors) {
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            errors.add("год должен быть числом");
            return;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (!primitive.isNumber()) {
            errors.add("год должен быть числом");
            return;
        }
        BigDecimal value = primitive.getAsBigDecimal().stripTrailingZeros();
        if (value.scale() > 0) {
            errors.add("год должен быть целым числом");
            return;
        }
        int max = maxYear();
        boolean inRange = value.precision() - value.scale() <= 9
                && value.compareTo(BigDecimal.valueOf(MIN_YEAR)) >= 0
                && value.compareTo(BigDecimal.valueOf(max)) <= 0;
        if (!inRange) {
            errors.add("год должен быть между " + MIN_YEAR + " и " + max);
        }
    }
}
