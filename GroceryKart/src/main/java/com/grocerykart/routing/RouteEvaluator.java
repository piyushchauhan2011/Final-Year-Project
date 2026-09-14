package com.grocerykart.routing;

import com.grocerykart.routing.model.*;
import java.util.*;

public final class RouteEvaluator {
  public static VehicleRoute evaluate(int vehicle, List<Integer> nodes, RoutingProblem p) {
    if (nodes.size() < 2) throw new IllegalArgumentException("route needs depot framing");
    long distance = 0;
    int load = 0, stops = nodes.size() - 2;
    for (int i = 0; i < nodes.size() - 1; i++)
      distance += p.distances().distance(nodes.get(i), nodes.get(i + 1));
    for (int i = 1; i < nodes.size() - 1; i++) load += p.stores().get(nodes.get(i)).demandKg();
    long travel = distance * 9L, service = (long) stops * p.constraints().serviceSecondsPerStop();
    return new VehicleRoute(vehicle, nodes, load, distance, travel, service, travel + service);
  }

  public static boolean feasible(VehicleRoute r, RoutingConstraints c) {
    return r.customerCount() <= c.maxStops()
        && r.loadKg() <= c.capacityKg()
        && r.distanceDeciKm() <= c.maxDistanceDeciKm()
        && r.durationSeconds() <= c.maxDurationSeconds();
  }
}
