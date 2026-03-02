job "o3000y" {
  datacenters = ["dc1"]
  type        = "service"

  group "app" {
    count = 0

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
        O3000Y_REST_PORT = "8081"
        O3000Y_GRPC_PORT = "4327"
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
