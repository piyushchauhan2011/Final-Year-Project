package com.grocerykart.routing;

import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.Assignment;
import com.google.ortools.constraintsolver.FirstSolutionStrategy;
import com.google.ortools.constraintsolver.LocalSearchMetaheuristic;
import com.google.ortools.constraintsolver.RoutingIndexManager;
import com.google.ortools.constraintsolver.RoutingModel;
import com.google.ortools.constraintsolver.main;
import com.grocerykart.routing.model.Algorithm;
import com.grocerykart.routing.model.ProgressPoint;
import com.grocerykart.routing.model.RoutingProblem;
import com.grocerykart.routing.model.RoutingSolution;
import com.grocerykart.routing.model.SolverRun;
import com.grocerykart.routing.model.Store;
import com.grocerykart.routing.model.VehicleRoute;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/** OR-Tools implementation of the same completed-route constraints as RouteEvaluator. */
public final class OrToolsVehicleRouter implements VehicleRouter {
  @Override
  public SolverRun run(
      RoutingProblem problem, ComparisonConfig config, SolverProgressListener listener) {
    Loader.loadNativeLibraries();
    long started = System.nanoTime();
    int customerCount = problem.stores().size() - 1;
    var manager =
        new RoutingIndexManager(problem.stores().size(), customerCount, problem.depotIndex());
    RoutingModel routing = new RoutingModel(manager);
    try {
      int distanceCallback =
          routing.registerTransitCallback(
              (from, to) ->
                  problem.distances().distance(manager.indexToNode(from), manager.indexToNode(to)));
      int demandCallback =
          routing.registerUnaryTransitCallback(
              index -> problem.stores().get(manager.indexToNode(index)).demandKg());
      int stopCallback =
          routing.registerUnaryTransitCallback(
              index -> manager.indexToNode(index) == problem.depotIndex() ? 0L : 1L);
      int timeCallback =
          routing.registerTransitCallback(
              (from, to) -> {
                int fromNode = manager.indexToNode(from);
                int toNode = manager.indexToNode(to);
                long travel = problem.distances().distance(fromNode, toNode) * 9L;
                return travel
                    + (fromNode == problem.depotIndex()
                        ? 0L
                        : problem.constraints().serviceSecondsPerStop());
              });
      routing.setArcCostEvaluatorOfAllVehicles(distanceCallback);
      routing.addDimension(
          distanceCallback, 0, problem.constraints().maxDistanceDeciKm(), true, "Distance");
      routing.addDimension(demandCallback, 0, problem.constraints().capacityKg(), true, "Capacity");
      routing.addDimension(stopCallback, 0, problem.constraints().maxStops(), true, "Stops");
      routing.addDimension(
          timeCallback, 0, problem.constraints().maxDurationSeconds(), true, "Time");
      routing.setFixedCostOfAllVehicles(
          (long) customerCount * problem.constraints().maxDistanceDeciKm() + 1);
      var parameters =
          main.defaultRoutingSearchParameters().toBuilder()
              .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PARALLEL_CHEAPEST_INSERTION)
              .setLocalSearchMetaheuristic(LocalSearchMetaheuristic.Value.GUIDED_LOCAL_SEARCH)
              .setTimeLimit(
                  com.google.protobuf.Duration.newBuilder()
                      .setSeconds(config.perSolverBudget().toSeconds())
                      .build())
              .build();
      Assignment assignment = routing.solveWithParameters(parameters);
      var elapsed = Duration.ofNanos(System.nanoTime() - started);
      if (assignment == null) {
        return new SolverRun(
            Algorithm.OR_TOOLS,
            OptionalLong.empty(),
            config.perSolverBudget(),
            elapsed,
            Optional.empty(),
            0,
            0,
            "NO_FEASIBLE_ASSIGNMENT",
            List.of(),
            List.of());
      }
      var routes = new ArrayList<VehicleRoute>();
      for (int vehicle = 0; vehicle < customerCount; vehicle++) {
        long index = routing.start(vehicle);
        if (routing.isEnd(assignment.value(routing.nextVar(index)))) continue;
        var nodes = new ArrayList<Integer>();
        while (!routing.isEnd(index)) {
          nodes.add(manager.indexToNode(index));
          index = assignment.value(routing.nextVar(index));
        }
        nodes.add(problem.depotIndex());
        routes.add(RouteEvaluator.evaluate(routes.size() + 1, nodes, problem));
      }
      long distance = routes.stream().mapToLong(VehicleRoute::distanceDeciKm).sum();
      int lowerBound =
          Math.max(
              (customerCount + problem.constraints().maxStops() - 1)
                  / problem.constraints().maxStops(),
              (problem.stores().stream().mapToInt(Store::demandKg).sum()
                      + problem.constraints().capacityKg()
                      - 1)
                  / problem.constraints().capacityKg());
      var solution =
          new RoutingSolution(
              Algorithm.OR_TOOLS,
              com.grocerykart.routing.model.SolutionStatus.FEASIBLE,
              routes,
              distance,
              lowerBound,
              routes.size() == lowerBound,
              elapsed,
              OptionalLong.empty());
      if (!SolutionValidator.validate(problem, solution).valid())
        throw new IllegalStateException("OR-Tools emitted invalid route");
      listener.onImprovement(solution);
      return new SolverRun(
          Algorithm.OR_TOOLS,
          OptionalLong.empty(),
          config.perSolverBudget(),
          elapsed,
          Optional.of(elapsed),
          1,
          0,
          "FEASIBLE",
          List.of(new ProgressPoint(elapsed, routes.size(), distance, 1, 0)),
          List.of(solution));
    } finally {
      routing.delete();
      manager.delete();
    }
  }
}
