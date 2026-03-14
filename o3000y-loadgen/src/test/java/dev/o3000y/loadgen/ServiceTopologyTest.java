package dev.o3000y.loadgen;

import static org.junit.jupiter.api.Assertions.*;

import dev.o3000y.loadgen.ServiceTopology.ServiceDef;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ServiceTopologyTest {

  @Test
  void defaultTopologyIsConsistent() {
    var topology = ServiceTopology.defaultTopology();
    Set<String> names = topology.stream().map(ServiceDef::name).collect(Collectors.toSet());

    // All downstream references must point to defined services
    for (ServiceDef svc : topology) {
      for (String downstream : svc.downstream()) {
        assertTrue(
            names.contains(downstream),
            svc.name() + " references undefined service: " + downstream);
      }
    }
  }

  @Test
  void topologyMapHasAllServices() {
    Map<String, ServiceDef> map = ServiceTopology.defaultTopologyMap();
    assertEquals(ServiceTopology.defaultTopology().size(), map.size());
  }

  @Test
  void latencyRangesAreValid() {
    for (ServiceDef svc : ServiceTopology.defaultTopology()) {
      assertTrue(
          svc.minLatencyUs() <= svc.maxLatencyUs(), svc.name() + " has invalid latency range");
      assertTrue(svc.minLatencyUs() > 0, svc.name() + " has non-positive min latency");
    }
  }
}
