package dev.tracequery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

/**
 * Tiny helper for sending JSON responses via JDK HttpServer.
 */
public class HttpUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        var bytes = MAPPER.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
