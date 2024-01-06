package org.example.utils;

import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class JSONBuilder {
    private final JSONObject header = new JSONObject();
    private Object body;

    private JSONBuilder() {
        header.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
    }

    public static JSONBuilder create() {
        return new JSONBuilder();
    }

    public JSONBuilder addHeader(String key, Object value) {
        header.put(key, value);
        return this;
    }

    public JSONBuilder setBody(Object content) {
        body = content;
        return this;
    }

    public JSONObject build() {
        JSONObject json = new JSONObject();
        json.put("header", header);
        json.put("body", body);

        return json;
    }

}