package com.grocerykart.routing.model;

import java.util.List;

public record VehicleRoute(
    int vehicleNumber,
    List<Integer> nodeIndices,
    int loadKg,
    long distanceDeciKm,
    long travelSeconds,
    long serviceSeconds,
    long durationSeconds) {
  public VehicleRoute {
    nodeIndices = List.copyOf(nodeIndices);
  }

  public int customerCount() {
    return Math.max(0, nodeIndices.size() - 2);
  }
}
