package com.grocerykart.routing.model;

public record RoutingConstraints(
    int maxStops,
    int capacityKg,
    int maxDistanceDeciKm,
    int maxDurationSeconds,
    int serviceSecondsPerStop) {
  public static final RoutingConstraints DEFAULT =
      new RoutingConstraints(20, 1000, 650, 10800, 300);

  public RoutingConstraints {
    if (maxStops < 1
        || capacityKg < 1
        || maxDistanceDeciKm < 1
        || maxDurationSeconds < 1
        || serviceSecondsPerStop < 0) throw new IllegalArgumentException("invalid constraints");
  }
}
