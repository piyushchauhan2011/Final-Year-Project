package com.grocerykart.routing;

import com.grocerykart.routing.model.*;
import java.util.*;

public final class SolutionValidator {
  public static ValidationResult validate(RoutingProblem p, RoutingSolution s) {
    var errors = new ArrayList<String>();
    var covered = new HashSet<Integer>();
    long total = 0;
    for (var r : s.routes()) {
      var n = r.nodeIndices();
      if (n.size() < 2 || n.getFirst() != p.depotIndex() || n.getLast() != p.depotIndex())
        errors.add("route not depot framed");
      for (int i = 1; i < n.size() - 1; i++) {
        int x = n.get(i);
        if (x == p.depotIndex() || x < 0 || x >= p.stores().size() || !covered.add(x))
          errors.add("invalid or duplicate customer");
      }
      try {
        var actual = RouteEvaluator.evaluate(r.vehicleNumber(), n, p);
        if (!actual.equals(r)) errors.add("stored route metrics disagree");
        if (!RouteEvaluator.feasible(actual, p.constraints()))
          errors.add("route constraint exceeded");
        total += actual.distanceDeciKm();
      } catch (RuntimeException e) {
        errors.add("invalid route node");
      }
    }
    for (var store : p.stores())
      if (store.index() != p.depotIndex() && !covered.contains(store.index()))
        errors.add("missing customer");
    if (total != s.totalDistanceDeciKm()) errors.add("solution total disagrees");
    return errors.isEmpty() ? ValidationResult.ok() : new ValidationResult(false, errors);
  }
}
