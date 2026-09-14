package com.grocerykart.routing;

import com.grocerykart.routing.model.*;
import java.util.*;

public record SolutionSignature(List<List<Integer>> routes)
    implements Comparable<SolutionSignature> {
  public SolutionSignature {
    routes = routes.stream().map(List::copyOf).sorted(SolutionSignature::compareRoute).toList();
  }

  public static SolutionSignature of(RoutingSolution s) {
    return new SolutionSignature(s.routes().stream().map(VehicleRoute::nodeIndices).toList());
  }

  private static int compareRoute(List<Integer> a, List<Integer> b) {
    for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
      int c = Integer.compare(a.get(i), b.get(i));
      if (c != 0) return c;
    }
    return Integer.compare(a.size(), b.size());
  }

  public int compareTo(SolutionSignature o) {
    for (int i = 0; i < Math.min(routes.size(), o.routes.size()); i++) {
      int c = compareRoute(routes.get(i), o.routes.get(i));
      if (c != 0) return c;
    }
    return Integer.compare(routes.size(), o.routes.size());
  }
}
