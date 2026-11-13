package com.cms.service;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public final class Passwords {
    private Passwords() {}

    public static boolean matches(String password, String stored) {
        if (password == null || password.isBlank() || stored == null) return false;
        return MessageDigest.isEqual(
                password.getBytes(StandardCharsets.UTF_8), stored.getBytes(StandardCharsets.UTF_8));
    }

    public static String validatePlainText(String password) {
        if (password == null || password.isBlank() || password.length() > 100) {
            throw new IllegalArgumentException("Enter a password of up to 100 characters");
        }
        return password;
    }
}
