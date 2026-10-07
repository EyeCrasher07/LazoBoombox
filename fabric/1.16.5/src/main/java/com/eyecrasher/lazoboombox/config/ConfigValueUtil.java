package com.eyecrasher.lazoboombox.config;

public final class ConfigValueUtil {
    private ConfigValueUtil() {}

    public static String stripComment(String value) {
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (quote == '"' && c == '\\') {
                escaped = true;
                continue;
            }
            if (quote != 0) {
                if (c == quote) quote = 0;
            } else if (c == '"' || c == '\'') quote = c;
            else if (c == '#') return value.substring(0, i).trim();
        }
        return value.trim();
    }

    public static String unquote(String value) {
        int length = value.length();
        if (length >= 2
                && ((value.charAt(0) == '"' && value.charAt(length - 1) == '"')
                        || (value.charAt(0) == '\'' && value.charAt(length - 1) == '\''))) {
            return value.substring(1, length - 1);
        }
        return value;
    }
}
