package com.foxykeep.cpcodegenerator.util;

import java.util.regex.Pattern;

public class NameUtils {

    private static final Pattern PATTERN = Pattern.compile("([a-z])([A-Z])");
    private static final String REPLACEMENT = "$1_$2";

    private NameUtils() {
    }

    public static String createConstantName(final String name) {
        return PATTERN.matcher(name).replaceAll(REPLACEMENT).toUpperCase();
    }

    public static String createLowerCamelCaseName(final String name) {
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }

    public static String createFieldName(final String name) {
        return "m" + name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
