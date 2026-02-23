package dev.o3000y.model;

public enum StatusCode {
  UNSET(0),
  OK(1),
  ERROR(2);

  private final int value;

  StatusCode(int value) {
    this.value = value;
  }

  public int value() {
    return value;
  }

  public static StatusCode fromValue(int value) {
    for (StatusCode code : values()) {
      if (code.value == value) {
        return code;
      }
    }
    return UNSET;
  }
}
