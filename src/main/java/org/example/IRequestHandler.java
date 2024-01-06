package org.example;

import org.example.Connection;
import org.json.JSONObject;

public interface IRequestHandler {
    void handle(JSONObject request, Connection connection) throws Exception;
}