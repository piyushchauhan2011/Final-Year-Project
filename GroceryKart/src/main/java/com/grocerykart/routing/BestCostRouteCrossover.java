package com.grocerykart.routing;

import com.grocerykart.routing.model.RoutingProblem;
import com.grocerykart.routing.model.RoutingSolution;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

/** Best-cost route crossover with feasible customer reinsertion. */
public final class BestCostRouteCrossover {
  public int[][] crossover(
      RoutingSolution first,
      RoutingSolution second,
      RoutingProblem problem,
      RandomGenerator random) {
    return new int[][] {
      child(first, selectedCustomers(second, random), problem, random),
      child(second, selectedCustomers(first, random), problem, random)
    };
  }

  private static List<Integer> selectedCustomers(RoutingSolution donor, RandomGenerator random) {
    var route = donor.routes().get(random.nextInt(donor.routes().size()));
    return new ArrayList<>(route.nodeIndices().subList(1, route.nodeIndices().size() - 1));
  }

  private static int[] child(
      RoutingSolution recipient,
      List<Integer> removedCustomers,
      RoutingProblem problem,
      RandomGenerator random) {
    var routes = new ArrayList<ArrayList<Integer>>();
    for (var route : recipient.routes()) {
      var customers =
          new ArrayList<>(route.nodeIndices().subList(1, route.nodeIndices().size() - 1));
      customers.removeAll(removedCustomers);
      if (!customers.isEmpty()) routes.add(customers);
    }
    shuffle(removedCustomers, random);
    for (int customer : removedCustomers) insertAtBestFeasiblePosition(routes, customer, problem);
    return routes.stream().flatMap(List::stream).mapToInt(Integer::intValue).toArray();
  }

  private static void insertAtBestFeasiblePosition(
      List<ArrayList<Integer>> routes, int customer, RoutingProblem problem) {
    int bestRoute = -1;
    int bestPosition = -1;
    long bestDelta = Long.MAX_VALUE;
    for (int routeIndex = 0; routeIndex < routes.size(); routeIndex++) {
      var route = routes.get(routeIndex);
      for (int position = 0; position <= route.size(); position++) {
        route.add(position, customer);
        var evaluated =
            RouteEvaluator.evaluate(0, depotFramed(route, problem.depotIndex()), problem);
        route.remove(position);
        if (!RouteEvaluator.feasible(evaluated, problem.constraints())) continue;
        long currentDistance =
            RouteEvaluator.evaluate(0, depotFramed(route, problem.depotIndex()), problem)
                .distanceDeciKm();
        long delta = evaluated.distanceDeciKm() - currentDistance;
        if (delta < bestDelta) {
          bestDelta = delta;
          bestRoute = routeIndex;
          bestPosition = position;
        }
      }
    }
    if (bestRoute < 0) routes.add(new ArrayList<>(List.of(customer)));
    else routes.get(bestRoute).add(bestPosition, customer);
  }

  private static List<Integer> depotFramed(List<Integer> customers, int depot) {
    var nodes = new ArrayList<Integer>(customers.size() + 2);
    nodes.add(depot);
    nodes.addAll(customers);
    nodes.add(depot);
    return nodes;
  }

  private static void shuffle(List<Integer> values, RandomGenerator random) {
    for (int index = values.size() - 1; index > 0; index--) {
      int other = random.nextInt(index + 1);
      Collections.swap(values, index, other);
    }
  }
}
