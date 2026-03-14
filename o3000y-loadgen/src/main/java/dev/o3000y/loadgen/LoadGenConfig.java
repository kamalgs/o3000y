package dev.o3000y.loadgen;

public record LoadGenConfig(
    String targetHost,
    int targetPort,
    double tracesPerSecond,
    int totalTraces,
    int durationSeconds,
    double errorRate,
    int maxDepth,
    int maxBreadth) {

  public static LoadGenConfig defaults() {
    return new LoadGenConfig("localhost", 4317, 10.0, 0, 60, 0.05, 5, 3);
  }

  public static LoadGenConfig fromArgs(String[] args) {
    String host = "localhost";
    int port = 4317;
    double tps = 10.0;
    int total = 0;
    int duration = 60;
    double errRate = 0.05;
    int depth = 5;
    int breadth = 3;

    for (String arg : args) {
      if (arg.startsWith("--")) {
        String[] parts = arg.substring(2).split("=", 2);
        if (parts.length != 2) continue;
        switch (parts[0]) {
          case "host" -> host = parts[1];
          case "port" -> port = Integer.parseInt(parts[1]);
          case "tps" -> tps = Double.parseDouble(parts[1]);
          case "total" -> total = Integer.parseInt(parts[1]);
          case "duration" -> duration = Integer.parseInt(parts[1]);
          case "error-rate" -> errRate = Double.parseDouble(parts[1]);
          case "max-depth" -> depth = Integer.parseInt(parts[1]);
          case "max-breadth" -> breadth = Integer.parseInt(parts[1]);
          default -> {
            /* ignore unknown */
          }
        }
      }
    }
    return new LoadGenConfig(host, port, tps, total, duration, errRate, depth, breadth);
  }
}
