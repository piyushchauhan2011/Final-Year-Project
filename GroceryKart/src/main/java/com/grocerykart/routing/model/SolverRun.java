package com.grocerykart.routing.model;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public record SolverRun(
    Algorithm algorithm,
    OptionalLong seed,
    Duration configuredBudget,
    Duration elapsed,
    Optional<Duration> firstFeasibleAt,
    long candidateEvaluations,
    long completedGenerations,
    String terminalStatus,
    List<ProgressPoint> progress,
    List<RoutingSolution> retainedSolutions) {
  public SolverRun {
    seed = seed == null ? OptionalLong.empty() : seed;
    firstFeasibleAt = firstFeasibleAt == null ? Optional.empty() : firstFeasibleAt;
    progress = List.copyOf(progress);
    retainedSolutions = List.copyOf(retainedSolutions);
  }
}
