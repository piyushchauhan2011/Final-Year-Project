package com.grocerykart.routing;

import com.grocerykart.routing.model.*;
import java.time.*;
import java.util.*;

public final class GiantTourDecoder {
  public static RoutingSolution decode(
      int[] order, RoutingProblem p, Algorithm algorithm, Duration foundAt, OptionalLong seed) {
    int n = order.length;
    int[] count = new int[n + 1];
    long[] dist = new long[n + 1];
    int[] previous = new int[n + 1];
    Arrays.fill(count, Integer.MAX_VALUE);
    Arrays.fill(dist, Long.MAX_VALUE);
    count[0] = 0;
    dist[0] = 0;
    for (int i = 0; i < n; i++)
      if (count[i] != Integer.MAX_VALUE) {
        var nodes = new ArrayList<Integer>();
        nodes.add(p.depotIndex());
        for (int j = i; j < n && j < i + p.constraints().maxStops(); j++) {
          nodes.add(order[j]);
          nodes.add(p.depotIndex());
          var route = RouteEvaluator.evaluate(0, nodes, p);
          nodes.removeLast();
          if (RouteEvaluator.feasible(route, p.constraints())) {
            int k = j + 1;
            long nd = dist[i] + route.distanceDeciKm();
            if (count[i] + 1 < count[k] || (count[i] + 1 == count[k] && nd < dist[k])) {
              count[k] = count[i] + 1;
              dist[k] = nd;
              previous[k] = i;
            }
          }
        }
      }
    if (count[n] == Integer.MAX_VALUE)
      throw new IllegalArgumentException("unserviceable permutation");
    var cuts = new ArrayList<Integer>();
    for (int at = n; at > 0; at = previous[at]) cuts.add(at);
    Collections.reverse(cuts);
    var routes = new ArrayList<VehicleRoute>();
    int start = 0, v = 1;
    for (int end : cuts) {
      var nodes = new ArrayList<Integer>();
      nodes.add(p.depotIndex());
      for (int i = start; i < end; i++) nodes.add(order[i]);
      nodes.add(p.depotIndex());
      routes.add(RouteEvaluator.evaluate(v++, nodes, p));
      start = end;
    }
    int lb =
        Math.max(
            (n + p.constraints().maxStops() - 1) / p.constraints().maxStops(),
            (p.stores().stream().mapToInt(Store::demandKg).sum() + p.constraints().capacityKg() - 1)
                / p.constraints().capacityKg());
    return new RoutingSolution(
        algorithm, SolutionStatus.FEASIBLE, routes, dist[n], lb, count[n] == lb, foundAt, seed);
  }
}
