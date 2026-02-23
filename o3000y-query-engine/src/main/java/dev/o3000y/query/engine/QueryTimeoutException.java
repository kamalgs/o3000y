package dev.o3000y.query.engine;

public final class QueryTimeoutException extends RuntimeException {

  public QueryTimeoutException(String sql, long timeoutMs) {
    super("Query timed out after " + timeoutMs + "ms: " + truncate(sql, 200));
  }

  private static String truncate(String s, int maxLen) {
    return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
  }
}
