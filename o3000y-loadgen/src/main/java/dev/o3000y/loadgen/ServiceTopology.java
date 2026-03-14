package dev.o3000y.loadgen;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ServiceTopology {

  public record ServiceDef(
      String name,
      List<String> operations,
      int spanKindValue,
      List<String> downstream,
      long minLatencyUs,
      long maxLatencyUs) {}

  private static final List<ServiceDef> DEFAULT =
      List.of(
          new ServiceDef(
              "api-gateway",
              List.of("GET /api/users", "POST /api/orders", "GET /api/products", "GET /api/health"),
              2, // SERVER
              List.of("user-service", "order-service", "product-service"),
              1_000,
              10_000),
          new ServiceDef(
              "user-service",
              List.of("getUserById", "authenticateUser", "updateProfile"),
              2,
              List.of("postgres-client", "redis-cache"),
              2_000,
              20_000),
          new ServiceDef(
              "order-service",
              List.of("createOrder", "getOrderHistory", "processPayment"),
              2,
              List.of("payment-service", "postgres-client", "kafka-producer"),
              5_000,
              50_000),
          new ServiceDef(
              "product-service",
              List.of("searchProducts", "getProductDetails", "listCategories"),
              2,
              List.of("elasticsearch-client", "redis-cache"),
              3_000,
              30_000),
          new ServiceDef(
              "payment-service",
              List.of("chargeCard", "refund", "validateCard"),
              2,
              List.of("stripe-client"),
              10_000,
              200_000),
          new ServiceDef(
              "postgres-client",
              List.of("SELECT", "INSERT", "UPDATE", "DELETE"),
              3, // CLIENT
              List.of(),
              500,
              50_000),
          new ServiceDef(
              "redis-cache", List.of("GET", "SET", "DEL", "MGET"), 3, List.of(), 100, 5_000),
          new ServiceDef(
              "kafka-producer",
              List.of("send order.created", "send payment.processed"),
              4, // PRODUCER
              List.of(),
              200,
              10_000),
          new ServiceDef(
              "elasticsearch-client",
              List.of("search products", "index product"),
              3,
              List.of(),
              1_000,
              100_000),
          new ServiceDef(
              "stripe-client",
              List.of("charges.create", "refunds.create"),
              3,
              List.of(),
              50_000,
              500_000));

  private ServiceTopology() {}

  public static List<ServiceDef> defaultTopology() {
    return DEFAULT;
  }

  public static Map<String, ServiceDef> defaultTopologyMap() {
    return DEFAULT.stream().collect(Collectors.toMap(ServiceDef::name, s -> s));
  }
}
