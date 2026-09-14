package com.grocerykart.routing.model;

import java.util.List;

public record Alternative(RoutingSolution representative, List<SolutionProvenance> provenance) {
  public Alternative {
    provenance = List.copyOf(provenance);
  }
}
