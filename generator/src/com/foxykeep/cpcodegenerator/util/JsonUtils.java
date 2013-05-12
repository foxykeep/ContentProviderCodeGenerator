package com.foxykeep.cpcodegenerator.util;

import org.json.JSONObject;

public class JsonUtils {

    private static final String JSON_NULL = "null";

    private JsonUtils() {}

    public static String getStringFixFalseNull(final JSONObject jsonObject, final String key) {
        if (jsonObject.has(key)) {
            final String value = jsonObject.optString(key);
            return JSON_NULL.equals(value) ? null : value;
        } else {
            return null;
        }
    }
}
