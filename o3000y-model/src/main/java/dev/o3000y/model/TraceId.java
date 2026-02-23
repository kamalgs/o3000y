package dev.o3000y.model;

public final class TraceId {

  private TraceId() {}

  public static String fromBytes(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      return "";
    }
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b & 0xFF));
    }
    return sb.toString();
  }

  public static byte[] toBytes(String hex) {
    if (hex == null || hex.isEmpty()) {
      return new byte[0];
    }
    int len = hex.length();
    byte[] bytes = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      bytes[i / 2] =
          (byte)
              ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
    }
    return bytes;
  }
}
