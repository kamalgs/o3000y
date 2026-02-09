package dev.tracequery;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws Exception {
        var dataDir = Path.of(env("DATA_DIR", "data/traces"));
        var port = Integer.parseInt(env("PORT", "7070"));

        var buffer = new SpanBuffer(dataDir);
        var ingest = new IngestHandler(buffer);
        var query = new QueryHandler(dataDir);

        var server = HttpServer.create(new InetSocketAddress(port), 0);

        // Ingestion (OTLP/HTTP JSON)
        server.createContext("/v1/traces", ingest::handle);

        // Query endpoints
        server.createContext("/api/v1/query", query::query);
        server.createContext("/api/v1/trace/", query::getTrace);
        server.createContext("/api/v1/services", query::listServices);
        server.createContext("/api/v1/operations", query::listOperations);

        Runtime.getRuntime().addShutdownHook(new Thread(buffer::flush));

        server.start();
        System.out.printf("TraceQuery listening on port %d%n", port);
        System.out.printf("  Ingest:  POST http://localhost:%d/v1/traces%n", port);
        System.out.printf("  Query:   POST http://localhost:%d/api/v1/query%n", port);
    }

    private static String env(String key, String defaultValue) {
        var val = System.getenv(key);
        return val != null && !val.isBlank() ? val : defaultValue;
    }
}
