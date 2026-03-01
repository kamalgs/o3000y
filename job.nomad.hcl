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
        image        = "o3000y:latest"
        network_mode = "host"
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
  }
}
