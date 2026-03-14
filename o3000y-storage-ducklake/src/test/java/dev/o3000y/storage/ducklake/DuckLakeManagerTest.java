package dev.o3000y.storage.ducklake;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DuckLakeManagerTest {

  @TempDir Path tempDir;
  private DuckLakeManager manager;

  @BeforeEach
  void setUp() {
    DuckLakeConfig config =
        new DuckLakeConfig(
            tempDir.resolve("metadata.ducklake").toString(),
            tempDir.resolve("files").toString() + "/");
    manager = new DuckLakeManager(config);
  }

  @AfterEach
  void tearDown() {
    manager.close();
  }

  @Test
  void tableCreated() throws Exception {
    try (Connection conn = manager.newConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT count(*) FROM spans")) {
      assertTrue(rs.next());
      assertEquals(0L, rs.getLong(1));
    }
  }

  @Test
  void newConnection_returnsWorkingConnection() throws Exception {
    Connection conn1 = manager.newConnection();
    Connection conn2 = manager.newConnection();
    assertNotNull(conn1);
    assertNotNull(conn2);

    // Both connections can query the same table
    try (Statement stmt1 = conn1.createStatement();
        ResultSet rs1 = stmt1.executeQuery("SELECT count(*) FROM spans")) {
      assertTrue(rs1.next());
    }
    try (Statement stmt2 = conn2.createStatement();
        ResultSet rs2 = stmt2.executeQuery("SELECT count(*) FROM spans")) {
      assertTrue(rs2.next());
    }

    conn1.close();
    conn2.close();
  }
}
