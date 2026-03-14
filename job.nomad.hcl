job "o3000y" {
  datacenters = ["dc1"]
  type        = "service"

  meta {
    deployed_at = "2026-03-14T13:30:00Z"
  }

  group "app" {
    count = 1

    network {
      mode = "host"
    }

    volume "o3000y_data" {
      type      = "host"
      source    = "o3000y_data"
      read_only = false
    }

    task "o3000y" {
      driver = "docker"

      config {
        image        = "o3000y:local"
        network_mode = "host"
      }

      env {
        O3000Y_REST_PORT            = "8083"
        O3000Y_GRPC_PORT            = "4327"
        O3000Y_DATA_PATH            = "/data/files/"
        O3000Y_CATALOG_URI          = "/data/metadata.ducklake"
        OTEL_SERVICE_NAME           = "o3000y"
        OTEL_EXPORTER_OTLP_ENDPOINT = "http://localhost:4318"
        OTEL_EXPORTER_OTLP_PROTOCOL = "http/protobuf"
        OTEL_LOGS_EXPORTER          = "otlp"
        OTEL_METRICS_EXPORTER       = "otlp"
        OTEL_TRACES_EXPORTER        = "otlp"
      }

      volume_mount {
        volume      = "o3000y_data"
        destination = "/data"
      }

      resources {
        cpu    = 1000
        memory = 1024
      }
    }

    task "telemetrygen" {
      driver = "docker"

      config {
        image        = "ghcr.io/open-telemetry/opentelemetry-collector-contrib/telemetrygen:latest"
        network_mode = "host"
        args = [
          "traces",
          "--otlp-endpoint=localhost:4327",
          "--otlp-insecure",
          "--traces=10",
          "--workers=2",
          "--rate=1",
        ]
      }

      lifecycle {
        hook    = "poststart"
        sidecar = false
      }

      resources {
        cpu    = 100
        memory = 64
      }
    }
  }
}
