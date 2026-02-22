package dev.o3000y.query.rest;

import java.util.List;

public record QueryResponse(
    List<String> columns, List<List<Object>> rows, int rowCount, long elapsedMs) {}
