package com.grocerykart.routing.model;

import java.time.Duration;
import java.util.List;
import java.util.OptionalLong;

public record RoutingSolution(
    Algorithm algorithm,
    SolutionStatus status,
    List<VehicleRoute> routes,
    long totalDistanceDeciKm,
    int fleetLowerBound,
    boolean minimumFleetProven,
    Duration foundAt,
    OptionalLong seed) {
  public RoutingSolution {
    routes = List.copyOf(routes);
    seed = seed == null ? OptionalLong.empty() : seed;
  }
}
