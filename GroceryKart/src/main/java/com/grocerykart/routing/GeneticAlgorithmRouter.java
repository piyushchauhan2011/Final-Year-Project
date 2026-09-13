package com.grocerykart.routing;

import com.grocerykart.routing.model.*;
import java.time.*;
import java.util.*;
import java.util.random.*;

public final class GeneticAlgorithmRouter implements VehicleRouter {
  private final Algorithm algorithm;
  private final boolean bcrc;

  public GeneticAlgorithmRouter(Algorithm algorithm) {
    if (algorithm != Algorithm.GA_PMX && algorithm != Algorithm.GA_BCRC)
      throw new IllegalArgumentException();
    this.algorithm = algorithm;
    this.bcrc = algorithm == Algorithm.GA_BCRC;
  }

  private record Candidate(int[] order, RoutingSolution solution) {}

  public SolverRun run(RoutingProblem p, ComparisonConfig cfg, SolverProgressListener listener) {
    long begin = System.nanoTime();
    long deadline = begin + cfg.perSolverBudget().toNanos();
    RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(cfg.gaSeed());
    int[] customers =
        p.stores().stream()
            .filter(s -> s.index() != p.depotIndex())
            .mapToInt(Store::index)
            .toArray();
    var pop = new ArrayList<Candidate>();
    var archive = new TreeMap<SolutionSignature, RoutingSolution>();
    long eval = 0;
    long generations = 0;
    Duration first = null;
    while (pop.size() < cfg.populationSize() && System.nanoTime() < deadline) {
      shuffle(customers, rng);
      var c = decode(customers, p, cfg, Duration.ofNanos(System.nanoTime() - begin));
      pop.add(c);
      eval++;
      first = archive(c, archive, listener, first);
    }
    if (pop.isEmpty())
      return new SolverRun(
          algorithm,
          OptionalLong.of(cfg.gaSeed()),
          cfg.perSolverBudget(),
          Duration.ofNanos(System.nanoTime() - begin),
          Optional.empty(),
          eval,
          generations,
          "BUDGET_EXHAUSTED_DURING_INITIALIZATION",
          List.of(),
          List.of());
    while (System.nanoTime() < deadline) {
      var next = new ArrayList<Candidate>(cfg.populationSize());
      while (next.size() < cfg.populationSize() && System.nanoTime() < deadline) {
        var a = best(pop, rng);
        var b = best(pop, rng);
        int[][] children =
            bcrc
                ? new BestCostRouteCrossover().crossover(a.solution, b.solution, p, rng)
                : new PmxCrossover().crossover(a.order, b.order, rng);
        for (int[] child : children) {
          if (next.size() == cfg.populationSize()) break;
          if (rng.nextDouble() < cfg.mutationRate()) mutate(child, rng);
          var c = decode(child, p, cfg, Duration.ofNanos(System.nanoTime() - begin));
          next.add(c);
          eval++;
          first = archive(c, archive, listener, first);
        }
      }
      if (next.size() != cfg.populationSize()) break;
      pop.addAll(next);
      pop.sort(
          Comparator.comparingInt((Candidate c) -> c.solution.routes().size())
              .thenComparingLong(c -> c.solution.totalDistanceDeciKm()));
      pop.subList(cfg.populationSize(), pop.size()).clear();
      generations++;
    }
    var elapsed = Duration.ofNanos(System.nanoTime() - begin);
    var progress = new ArrayList<ProgressPoint>();
    for (var s : archive.values())
      progress.add(
          new ProgressPoint(
              s.foundAt(), s.routes().size(), s.totalDistanceDeciKm(), eval, generations));
    return new SolverRun(
        algorithm,
        OptionalLong.of(cfg.gaSeed()),
        cfg.perSolverBudget(),
        elapsed,
        Optional.ofNullable(first),
        eval,
        generations,
        "BUDGET_EXHAUSTED",
        progress,
        List.copyOf(archive.values()));
  }

  private Candidate decode(int[] order, RoutingProblem p, ComparisonConfig c, Duration at) {
    var solution =
        GiantTourDecoder.decode(order.clone(), p, algorithm, at, OptionalLong.of(c.gaSeed()));
    var validation = SolutionValidator.validate(p, solution);
    if (!validation.valid())
      throw new IllegalStateException("decoder emitted invalid solution: " + validation.errors());
    return new Candidate(order.clone(), solution);
  }

  private static Candidate best(List<Candidate> p, RandomGenerator rng) {
    var a = p.get(rng.nextInt(p.size()));
    var b = p.get(rng.nextInt(p.size()));
    return Comparator.comparingInt((Candidate x) -> x.solution.routes().size())
                .thenComparingLong(x -> x.solution.totalDistanceDeciKm())
                .compare(a, b)
            <= 0
        ? a
        : b;
  }

  private static void shuffle(int[] a, RandomGenerator r) {
    for (int i = a.length - 1; i > 0; i--) {
      int j = r.nextInt(i + 1);
      int x = a[i];
      a[i] = a[j];
      a[j] = x;
    }
  }

  private static void mutate(int[] a, RandomGenerator r) {
    if (a.length < 2) return;
    int i = r.nextInt(a.length - 1),
        j = Math.min(a.length - 1, i + 1 + r.nextInt(Math.min(2, a.length - i - 1)));
    while (i < j) {
      int x = a[i];
      a[i++] = a[j];
      a[j--] = x;
    }
  }

  private static Duration archive(
      Candidate candidate,
      NavigableMap<SolutionSignature, RoutingSolution> archive,
      SolverProgressListener listener,
      Duration firstFeasible) {
    var signature = SolutionSignature.of(candidate.solution);
    if (archive.containsKey(signature)) return firstFeasible;
    int vehicles = candidate.solution.routes().size();
    long distance = candidate.solution.totalDistanceDeciKm();
    boolean dominated =
        archive.values().stream()
            .anyMatch(
                existing ->
                    dominates(
                        existing.routes().size(),
                        existing.totalDistanceDeciKm(),
                        vehicles,
                        distance));
    if (dominated) return firstFeasible;
    archive
        .entrySet()
        .removeIf(
            entry ->
                dominates(
                    vehicles,
                    distance,
                    entry.getValue().routes().size(),
                    entry.getValue().totalDistanceDeciKm()));
    archive.put(signature, candidate.solution);
    while (archive.size() > 50) archive.pollLastEntry();
    listener.onImprovement(candidate.solution);
    return firstFeasible == null ? candidate.solution.foundAt() : firstFeasible;
  }

  private static boolean dominates(
      int leftVehicles, long leftDistance, int rightVehicles, long rightDistance) {
    return leftVehicles <= rightVehicles
        && leftDistance <= rightDistance
        && (leftVehicles < rightVehicles || leftDistance < rightDistance);
  }
}
