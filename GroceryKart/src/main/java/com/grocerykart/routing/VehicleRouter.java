package com.grocerykart.routing;

import com.grocerykart.routing.model.*;

public interface VehicleRouter {
  SolverRun run(RoutingProblem problem, ComparisonConfig config, SolverProgressListener listener);
}
