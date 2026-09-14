package com.grocerykart.routing;

import com.grocerykart.routing.model.RoutingSolution;

@FunctionalInterface
public interface SolverProgressListener {
  void onImprovement(RoutingSolution solution);
}
