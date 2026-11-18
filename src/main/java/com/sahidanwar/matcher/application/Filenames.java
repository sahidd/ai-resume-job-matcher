package com.sahidanwar.matcher.application;

public final class Filenames {

    private Filenames() {
    }

    public static String sanitize(String original) {
        String name = original == null ? "" : original.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replace("\u0000", "").replaceAll("[\\r\\n]", "").trim();
        if (name.isBlank()) {
            name = "upload.txt";
        }
        return name.length() <= 255 ? name : name.substring(name.length() - 255);
    }
}
