package com.grocerykart.routing.model;

import java.util.List;

public record ComparisonResult(
    RoutingProblem problem, List<SolverRun> runs, List<Alternative> alternatives) {
  public ComparisonResult {
    runs = List.copyOf(runs);
    alternatives = List.copyOf(alternatives);
  }
}
