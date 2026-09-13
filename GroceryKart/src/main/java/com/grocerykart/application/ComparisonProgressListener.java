package com.grocerykart.application;

import com.grocerykart.routing.model.Algorithm;
import com.grocerykart.routing.model.RoutingSolution;
import com.grocerykart.routing.model.SolverRun;

public interface ComparisonProgressListener {
  ComparisonProgressListener NONE = new ComparisonProgressListener() {};

  default void onSolverStarted(Algorithm algorithm, int position, int total) {}

  default void onImprovement(RoutingSolution solution) {}

  default void onSolverFinished(SolverRun run, int completed, int total) {}
}
