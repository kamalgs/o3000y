package dev.o3000y.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TraceIdTest {

  @Test
  void roundTripHexConversion() {
    String hex = "0af7651916cd43dd8448eb211c80319c";
    byte[] bytes = TraceId.toBytes(hex);
    String result = TraceId.fromBytes(bytes);
    assertEquals(hex, result);
  }

  @Test
  void fromBytes_emptyArray_returnsEmptyString() {
    assertEquals("", TraceId.fromBytes(new byte[0]));
  }

  @Test
  void fromBytes_null_returnsEmptyString() {
    assertEquals("", TraceId.fromBytes(null));
  }

  @Test
  void toBytes_emptyString_returnsEmptyArray() {
    assertArrayEquals(new byte[0], TraceId.toBytes(""));
  }

  @Test
  void toBytes_null_returnsEmptyArray() {
    assertArrayEquals(new byte[0], TraceId.toBytes(null));
  }

  @Test
  void roundTripSpanId() {
    String hex = "00f067aa0ba902b7";
    byte[] bytes = TraceId.toBytes(hex);
    assertEquals(8, bytes.length);
    assertEquals(hex, TraceId.fromBytes(bytes));
  }
}
