package com.condos.user.util;

import java.security.SecureRandom;

public final class Passwords {
    private static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*";

    private static final SecureRandom RANDOM = new SecureRandom();

    private Passwords() {}

    public static String generateSecure(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}