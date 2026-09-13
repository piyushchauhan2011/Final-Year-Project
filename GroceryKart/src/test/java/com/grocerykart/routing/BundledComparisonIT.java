package com.grocerykart.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grocerykart.application.ComparisonProgressListener;
import com.grocerykart.application.ComparisonService;
import com.grocerykart.report.CostCalculator;
import com.grocerykart.report.PdfReportExporter;
import com.grocerykart.routing.io.DatasetLoader;
import com.grocerykart.routing.model.Algorithm;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class BundledComparisonIT {
  @Test
  void allSolversProduceCompleteValidSolutionsForBundledData() throws Exception {
    var problem =
        new DatasetLoader()
            .load(
                Path.of("src/main/resources/data/DemandTable.txt"),
                Path.of("src/main/resources/data/DistanceMatrix.txt"));
    assertEquals(601, problem.stores().size());
    assertEquals("DC", problem.depot().code());
    assertEquals(68, problem.distances().distance(problem.depotIndex(), 0));

    var started = new java.util.ArrayList<Algorithm>();
    var finished = new java.util.ArrayList<Algorithm>();
    var improvements = new java.util.concurrent.atomic.AtomicInteger();
    var progress =
        new ComparisonProgressListener() {
          @Override
          public void onSolverStarted(Algorithm algorithm, int position, int total) {
            started.add(algorithm);
          }

          @Override
          public void onImprovement(com.grocerykart.routing.model.RoutingSolution solution) {
            improvements.incrementAndGet();
          }

          @Override
          public void onSolverFinished(
              com.grocerykart.routing.model.SolverRun run, int completed, int total) {
            finished.add(run.algorithm());
          }
        };
    var comparison =
        new ComparisonService()
            .compare(
                problem,
                new ComparisonConfig(Duration.ofSeconds(5), 20, 0.9, 0.1, 42, 10),
                progress);

    assertEquals(3, comparison.runs().size());
    assertEquals(
        java.util.List.of(Algorithm.GA_PMX, Algorithm.GA_BCRC, Algorithm.OR_TOOLS), started);
    assertEquals(started, finished);
    assertTrue(improvements.get() >= 3);
    for (var run : comparison.runs()) {
      var best =
          run.retainedSolutions().stream()
              .min(
                  Comparator.comparingInt(
                          (com.grocerykart.routing.model.RoutingSolution solution) ->
                              solution.routes().size())
                      .thenComparingLong(
                          com.grocerykart.routing.model.RoutingSolution::totalDistanceDeciKm))
              .orElseThrow();
      System.out.printf(
          "%s status=%s retained=%d trucks=%d distance=%.1f km elapsed=%s%n",
          run.algorithm(),
          run.terminalStatus(),
          run.retainedSolutions().size(),
          best.routes().size(),
          best.totalDistanceDeciKm() / 10.0,
          run.elapsed());
      assertFalse(run.retainedSolutions().isEmpty(), run.algorithm() + ": " + run.terminalStatus());
      assertTrue(run.retainedSolutions().size() <= 50);
      for (var solution : run.retainedSolutions()) {
        assertTrue(SolutionValidator.validate(problem, solution).valid(), run.algorithm().name());
      }
    }
    assertTrue(
        comparison.runs().stream().map(run -> run.algorithm()).anyMatch(Algorithm.GA_PMX::equals));
    assertFalse(comparison.alternatives().isEmpty());
    var selected = comparison.alternatives().getFirst();
    var selectedCost =
        CostCalculator.calculate(comparison.alternatives().getFirst().representative())
            .threeYearOperating();
    assertTrue(
        comparison.alternatives().stream()
            .allMatch(
                alternative ->
                    selectedCost.compareTo(
                            CostCalculator.calculate(alternative.representative())
                                .threeYearOperating())
                        <= 0));
    var report = Files.createTempFile("grocerykart-verification-", ".pdf");
    try {
      new PdfReportExporter().export(comparison, selected, report);
      try (var document = Loader.loadPDF(report.toFile())) {
        var text = new PDFTextStripper().getText(document);
        assertTrue(text.contains("Number of trucks needed:"));
        assertTrue(text.contains("Total distance travelled each day:"));
        assertTrue(text.contains("Routing for each truck"));
        assertTrue(text.contains("Route: DC ->"));
      }
    } finally {
      Files.deleteIfExists(report);
    }
  }
}
