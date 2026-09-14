package com.grocerykart.application;

import com.grocerykart.report.CostCalculator;
import com.grocerykart.routing.*;
import com.grocerykart.routing.model.*;
import java.util.*;

public final class ComparisonService {
  public ComparisonResult compare(
      RoutingProblem problem,
      ComparisonConfig config,
      ComparisonProgressListener progressListener) {
    var runs = new ArrayList<SolverRun>();
    var routers =
        List.<VehicleRouter>of(
            new GeneticAlgorithmRouter(Algorithm.GA_PMX),
            new GeneticAlgorithmRouter(Algorithm.GA_BCRC),
            new OrToolsVehicleRouter());
    var algorithms = List.of(Algorithm.GA_PMX, Algorithm.GA_BCRC, Algorithm.OR_TOOLS);
    for (int index = 0; index < routers.size(); index++) {
      var router = routers.get(index);
      var algorithm = algorithms.get(index);
      progressListener.onSolverStarted(algorithm, index + 1, routers.size());
      SolverRun run;
      try {
        run =
            router.run(
                problem,
                config,
                solution -> {
                  if (!SolutionValidator.validate(problem, solution).valid()) {
                    throw new IllegalStateException("invalid solver callback");
                  }
                  progressListener.onImprovement(solution);
                });
      } catch (Exception failure) {
        run =
            new SolverRun(
                algorithm,
                OptionalLong.empty(),
                config.perSolverBudget(),
                java.time.Duration.ZERO,
                Optional.empty(),
                0,
                0,
                "FAILED: " + failure.getMessage(),
                List.of(),
                List.of());
      }
      runs.add(run);
      progressListener.onSolverFinished(run, index + 1, routers.size());
    }
    var grouped = new TreeMap<SolutionSignature, List<RoutingSolution>>();
    for (var r : runs)
      for (var s : r.retainedSolutions())
        grouped.computeIfAbsent(SolutionSignature.of(s), x -> new ArrayList<>()).add(s);
    var alternatives = new ArrayList<Alternative>();
    for (var list : grouped.values()) {
      var rep =
          list.stream()
              .sorted(
                  Comparator.comparing(RoutingSolution::status)
                      .thenComparing(RoutingSolution::foundAt)
                      .thenComparing(RoutingSolution::algorithm))
              .findFirst()
              .orElseThrow();
      var provenance =
          list.stream()
              .map(s -> new SolutionProvenance(s.algorithm(), s.seed(), s.foundAt()))
              .toList();
      alternatives.add(new Alternative(rep, provenance));
    }
    alternatives.removeIf(
        a ->
            alternatives.stream()
                .anyMatch(
                    b ->
                        b != a
                            && b.representative().routes().size()
                                <= a.representative().routes().size()
                            && b.representative().totalDistanceDeciKm()
                                <= a.representative().totalDistanceDeciKm()
                            && (b.representative().routes().size()
                                    < a.representative().routes().size()
                                || b.representative().totalDistanceDeciKm()
                                    < a.representative().totalDistanceDeciKm())));
    alternatives.sort(
        Comparator.comparing(
                (Alternative alternative) ->
                    CostCalculator.calculate(alternative.representative()).threeYearOperating())
            .thenComparingInt(alternative -> alternative.representative().routes().size())
            .thenComparingLong(alternative -> alternative.representative().totalDistanceDeciKm()));
    return new ComparisonResult(
        problem, runs, alternatives.subList(0, Math.min(10, alternatives.size())));
  }
}
