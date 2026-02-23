package dev.o3000y.query.engine;

import java.util.List;

public record QueryResult(
    List<String> columns, List<List<Object>> rows, int rowCount, long elapsedMs) {}
