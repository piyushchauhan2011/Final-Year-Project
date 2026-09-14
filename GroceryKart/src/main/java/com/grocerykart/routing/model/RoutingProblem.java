package com.grocerykart.routing.model;

import java.util.List;

public record RoutingProblem(
    List<Store> stores, int depotIndex, DistanceMatrix distances, RoutingConstraints constraints) {
  public RoutingProblem {
    stores = List.copyOf(stores);
    if (distances.size() != stores.size() || depotIndex < 0 || depotIndex >= stores.size())
      throw new IllegalArgumentException("inconsistent problem");
  }

  public Store depot() {
    return stores.get(depotIndex);
  }
}
