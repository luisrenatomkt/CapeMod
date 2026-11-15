package com.capemod.server.security;

import java.util.regex.Pattern;

public final class InputValidator {
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,16}$");
    private static final Pattern UUID = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    private static final Pattern SHA256 = Pattern.compile("^[0-9a-f]{64}$");

    private InputValidator() {}

    public static boolean isUsername(String s) { return s != null && USERNAME.matcher(s).matches(); }
    public static boolean isPassword(String s) { return s != null && s.length() >= 8 && s.length() <= 128; }
    public static boolean isUuid(String s) { return s != null && UUID.matcher(s).matches(); }
    public static boolean isSha256(String s) { return s != null && SHA256.matcher(s).matches(); }
}
