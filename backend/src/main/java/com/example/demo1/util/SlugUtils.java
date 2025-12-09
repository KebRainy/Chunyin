package com.example.demo1.util;

import java.text.Normalizer;

public final class SlugUtils {

    private SlugUtils() {
    }

    public static String toSlug(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized
            .toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .trim()
            .replaceAll("\\s+", "-");
    }
}

