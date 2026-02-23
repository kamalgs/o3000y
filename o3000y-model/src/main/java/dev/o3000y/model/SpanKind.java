package dev.o3000y.model;

public enum SpanKind {
  UNSPECIFIED(0),
  INTERNAL(1),
  SERVER(2),
  CLIENT(3),
  PRODUCER(4),
  CONSUMER(5);

  private final int value;

  SpanKind(int value) {
    this.value = value;
  }

  public int value() {
    return value;
  }

  public static SpanKind fromValue(int value) {
    for (SpanKind kind : values()) {
      if (kind.value == value) {
        return kind;
      }
    }
    return UNSPECIFIED;
  }
}
