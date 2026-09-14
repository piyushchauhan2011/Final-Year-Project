package com.grocerykart.routing;

import java.time.Duration;

public record ComparisonConfig(
    Duration perSolverBudget,
    int populationSize,
    double crossoverRate,
    double mutationRate,
    long gaSeed,
    int maximumAlternatives) {
  public static final ComparisonConfig DEFAULT =
      new ComparisonConfig(Duration.ofSeconds(60), 200, .90, .10, 42, 10);

  public ComparisonConfig {
    if (perSolverBudget.compareTo(Duration.ofSeconds(5)) < 0
        || perSolverBudget.compareTo(Duration.ofMinutes(10)) > 0
        || populationSize < 20
        || populationSize > 2000
        || crossoverRate < 0
        || crossoverRate > 1
        || mutationRate < 0
        || mutationRate > 1
        || maximumAlternatives != 10)
      throw new IllegalArgumentException("invalid comparison configuration");
  }
}
