package com.grocerykart.app;

import com.grocerykart.application.ComparisonProgressListener;
import com.grocerykart.report.CostCalculator;
import com.grocerykart.report.PdfReportExporter;
import com.grocerykart.routing.ComparisonConfig;
import com.grocerykart.routing.model.Algorithm;
import com.grocerykart.routing.model.Alternative;
import com.grocerykart.routing.model.ComparisonResult;
import java.time.Duration;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

public final class MainView {
  private final RoutingController controller = new RoutingController();
  private ComparisonResult result;

  public Parent root() {
    var budget = new Spinner<Integer>(5, 600, 60);
    var population = new Spinner<Integer>(20, 2000, 200);
    var crossover = new TextField("0.90");
    var mutation = new TextField("0.10");
    var seed = new TextField("42");
    var run = new Button("Run Comparison");
    var export = new Button("Export selected PDF");
    export.setDisable(true);
    var status =
        new Label(
            "Bundled data selected. Constraints: 20 stops, 1,000 kg, 65.0 km, 10,800 seconds.");
    status.setWrapText(true);
    var summary = new Label("Run the comparison and select a solution.");
    summary.setWrapText(true);
    var runDetails = new TextArea("No comparison has been run.");
    runDetails.setEditable(false);
    runDetails.setWrapText(true);
    runDetails.setPrefRowCount(8);
    var comparisonProgress = new ProgressBar(0);
    comparisonProgress.setMaxWidth(Double.MAX_VALUE);
    var progressText = new Label("Ready.");
    progressText.setWrapText(true);

    var alternatives = new ListView<Alternative>();
    alternatives.setCellFactory(
        view ->
            new ListCell<>() {
              @Override
              protected void updateItem(Alternative item, boolean empty) {
                super.updateItem(item, empty);
                setText(
                    empty || item == null
                        ? null
                        : item.representative().algorithm()
                            + " — "
                            + item.representative().routes().size()
                            + " trucks, "
                            + kilometres(item.representative().totalDistanceDeciKm())
                            + " km per day");
              }
            });
    var routes = new TextArea("Select an alternative.");
    routes.setEditable(false);
    routes.setWrapText(true);
    var stores = new TextArea("Select an alternative.");
    stores.setEditable(false);
    stores.setWrapText(true);

    alternatives
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (ignored, old, selected) -> {
              export.setDisable(selected == null);
              if (selected == null || result == null) return;
              var solution = selected.representative();
              var costs = CostCalculator.calculate(solution);
              summary.setText(
                  "Selected solution: "
                      + solution.algorithm()
                      + "\nNumber of trucks needed: "
                      + solution.routes().size()
                      + "\nTotal distance travelled each day: "
                      + kilometres(solution.totalDistanceDeciKm())
                      + " km"
                      + "\nDaily variable cost: Rs. "
                      + costs.dailyVariable().toPlainString()
                      + "\nMonthly fleet maintenance: Rs. "
                      + costs.monthlyMaintenance().toPlainString()
                      + "\nTotal operating cost for first three years: Rs. "
                      + costs.threeYearOperating().toPlainString()
                      + "\nAll routes independently validated against capacity, stops, distance, return-to-warehouse, and the 5:00–8:00 time window.");
              var routeText = new StringBuilder();
              var storeText = new StringBuilder();
              for (var route : selected.representative().routes()) {
                routeText.append("Truck ").append(route.vehicleNumber()).append(": ");
                for (int index = 0; index < route.nodeIndices().size(); index++) {
                  if (index > 0) routeText.append(" -> ");
                  routeText.append(
                      result.problem().stores().get(route.nodeIndices().get(index)).code());
                }
                routeText
                    .append("\nLoad: ")
                    .append(route.loadKg())
                    .append(" kg; stops: ")
                    .append(route.customerCount())
                    .append("; completed distance: ")
                    .append(kilometres(route.distanceDeciKm()))
                    .append(" km; total duration: ")
                    .append(route.durationSeconds() / 60)
                    .append(" minutes\n\n");
                for (int position = 1; position < route.nodeIndices().size() - 1; position++) {
                  var store = result.problem().stores().get(route.nodeIndices().get(position));
                  storeText
                      .append(store.code())
                      .append(": ")
                      .append(store.demandKg())
                      .append(" kg — vehicle ")
                      .append(route.vehicleNumber())
                      .append(", visit ")
                      .append(position)
                      .append('\n');
                }
              }
              routes.setText(routeText.toString());
              stores.setText(storeText.toString());
            });

    export.setOnAction(event -> exportSelected(alternatives, export, status));
    run.setOnAction(
        event ->
            runComparison(
                budget,
                population,
                crossover,
                mutation,
                seed,
                run,
                export,
                status,
                runDetails,
                comparisonProgress,
                progressText,
                alternatives));

    var form = new GridPane();
    form.setHgap(8);
    form.setVgap(8);
    form.addRow(0, new Label("Per-solver seconds"), budget, new Label("Population"), population);
    form.addRow(
        1,
        new Label("Crossover"),
        crossover,
        new Label("Mutation"),
        mutation,
        new Label("Seed"),
        seed,
        run,
        export);
    var tabs =
        new TabPane(
            new Tab("Comparison", new VBox(12, status, summary, runDetails)),
            new Tab("Alternatives", alternatives),
            new Tab("Routes", routes),
            new Tab("Stores", stores));
    tabs.getTabs().forEach(tab -> tab.setClosable(false));
    var root = new VBox(12, form, comparisonProgress, progressText, tabs);
    root.setPadding(new Insets(18));
    VBox.setVgrow(tabs, Priority.ALWAYS);
    return root;
  }

  private void runComparison(
      Spinner<Integer> budget,
      Spinner<Integer> population,
      TextField crossover,
      TextField mutation,
      TextField seed,
      Button run,
      Button export,
      Label status,
      TextArea runDetails,
      ProgressBar comparisonProgress,
      Label progressText,
      ListView<Alternative> alternatives) {
    try {
      var config =
          new ComparisonConfig(
              Duration.ofSeconds(budget.getValue()),
              population.getValue(),
              Double.parseDouble(crossover.getText()),
              Double.parseDouble(mutation.getText()),
              Long.parseLong(seed.getText()),
              10);
      run.setDisable(true);
      export.setDisable(true);
      comparisonProgress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
      progressText.setText("Loading and validating the 600-store dataset…");
      status.setText(
          "Comparison runs three solvers sequentially. With "
              + budget.getValue()
              + " seconds per solver, the search takes about "
              + (budget.getValue() * 3)
              + " seconds plus setup.");

      var activeAlgorithm = new Algorithm[1];
      var activePosition = new int[1];
      var solverStartedAt = new long[1];
      var improvements = new int[1];
      var ticker =
          new javafx.animation.Timeline(
              new javafx.animation.KeyFrame(
                  javafx.util.Duration.seconds(1),
                  ignored -> {
                    if (activeAlgorithm[0] == null) return;
                    double elapsedSeconds =
                        (System.nanoTime() - solverStartedAt[0]) / 1_000_000_000.0;
                    double solverFraction =
                        Math.min(1.0, elapsedSeconds / config.perSolverBudget().toSeconds());
                    comparisonProgress.setProgress(
                        ((activePosition[0] - 1) + solverFraction) / 3.0);
                    progressText.setText(
                        "Running "
                            + displayName(activeAlgorithm[0])
                            + " ("
                            + activePosition[0]
                            + " of 3): "
                            + String.format(
                                java.util.Locale.ROOT,
                                "%.0f / %d seconds; %d valid improvements found",
                                elapsedSeconds,
                                config.perSolverBudget().toSeconds(),
                                improvements[0]));
                  }));
      ticker.setCycleCount(javafx.animation.Animation.INDEFINITE);
      ticker.play();

      var progressListener =
          new ComparisonProgressListener() {
            @Override
            public void onSolverStarted(Algorithm algorithm, int position, int total) {
              Platform.runLater(
                  () -> {
                    activeAlgorithm[0] = algorithm;
                    activePosition[0] = position;
                    solverStartedAt[0] = System.nanoTime();
                    improvements[0] = 0;
                    comparisonProgress.setProgress((position - 1.0) / total);
                    progressText.setText(
                        "Starting "
                            + displayName(algorithm)
                            + " ("
                            + position
                            + " of "
                            + total
                            + ")…");
                  });
            }

            @Override
            public void onImprovement(com.grocerykart.routing.model.RoutingSolution solution) {
              Platform.runLater(
                  () -> {
                    improvements[0]++;
                    progressText.setText(
                        "Running "
                            + displayName(solution.algorithm())
                            + ": valid improvement "
                            + improvements[0]
                            + " — "
                            + solution.routes().size()
                            + " trucks, "
                            + kilometres(solution.totalDistanceDeciKm())
                            + " km/day");
                  });
            }

            @Override
            public void onSolverFinished(
                com.grocerykart.routing.model.SolverRun solverRun, int completed, int total) {
              Platform.runLater(
                  () -> {
                    comparisonProgress.setProgress(completed / (double) total);
                    progressText.setText(
                        displayName(solverRun.algorithm())
                            + " finished: "
                            + readableStatus(solverRun.terminalStatus())
                            + ". "
                            + completed
                            + " of "
                            + total
                            + " solvers complete.");
                  });
            }
          };

      controller
          .compareBundled(config, progressListener)
          .whenComplete(
              (comparison, failure) ->
                  Platform.runLater(
                      () -> {
                        ticker.stop();
                        run.setDisable(false);
                        if (failure != null) {
                          comparisonProgress.setProgress(0);
                          progressText.setText("Comparison failed.");
                          status.setText("Comparison failed: " + failure.getCause());
                        } else {
                          result = comparison;
                          alternatives.getItems().setAll(comparison.alternatives());
                          alternatives.getSelectionModel().selectFirst();
                          var details = new StringBuilder("Solver results\n");
                          for (var solverRun : comparison.runs()) {
                            details
                                .append(displayName(solverRun.algorithm()))
                                .append(": ")
                                .append(readableStatus(solverRun.terminalStatus()))
                                .append("; elapsed ")
                                .append(
                                    String.format(
                                        java.util.Locale.ROOT,
                                        "%.2f seconds",
                                        solverRun.elapsed().toNanos() / 1_000_000_000.0));
                            solverRun.retainedSolutions().stream()
                                .min(
                                    java.util.Comparator.comparingInt(
                                            (com.grocerykart.routing.model.RoutingSolution
                                                    solution) -> solution.routes().size())
                                        .thenComparingLong(
                                            com.grocerykart.routing.model.RoutingSolution
                                                ::totalDistanceDeciKm))
                                .ifPresent(
                                    best ->
                                        details
                                            .append("; best ")
                                            .append(best.routes().size())
                                            .append(" trucks, ")
                                            .append(kilometres(best.totalDistanceDeciKm()))
                                            .append(" km/day"));
                            details.append('\n');
                          }
                          runDetails.setText(details.toString());
                          comparisonProgress.setProgress(1);
                          progressText.setText(
                              "Comparison complete. Select an alternative to inspect routes or export the PDF.");
                          status.setText(
                              "Completed "
                                  + comparison.runs().size()
                                  + " solver runs; "
                                  + comparison.alternatives().size()
                                  + " non-dominated alternatives.");
                        }
                      }));
    } catch (RuntimeException failure) {
      comparisonProgress.setProgress(0);
      progressText.setText("Comparison did not start.");
      status.setText("Invalid configuration: " + failure.getMessage());
    }
  }

  private void exportSelected(ListView<Alternative> alternatives, Button export, Label status) {
    var selected = alternatives.getSelectionModel().getSelectedItem();
    if (selected == null || result == null) return;
    var chooser = new FileChooser();
    chooser.setInitialFileName("GroceryKart-report.pdf");
    var file = chooser.showSaveDialog(export.getScene().getWindow());
    if (file == null) return;
    try {
      new PdfReportExporter().export(result, selected, file.toPath());
      status.setText("Exported " + file.getName());
    } catch (Exception failure) {
      status.setText("Export failed: " + failure.getMessage());
    }
  }

  private static String displayName(Algorithm algorithm) {
    return switch (algorithm) {
      case GA_PMX -> "Genetic Algorithm (PMX)";
      case GA_BCRC -> "Genetic Algorithm (BCRC)";
      case OR_TOOLS -> "Google OR-Tools";
    };
  }

  private static String readableStatus(String status) {
    return switch (status) {
      case "BUDGET_EXHAUSTED" -> "configured search time completed";
      case "BUDGET_EXHAUSTED_DURING_INITIALIZATION" ->
          "search time completed during population initialization";
      case "FEASIBLE" -> "feasible solution found";
      case "NO_FEASIBLE_ASSIGNMENT" -> "no feasible solution found within the time limit";
      default -> status;
    };
  }

  private static String kilometres(long deciKilometres) {
    return String.format(java.util.Locale.ROOT, "%.1f", deciKilometres / 10.0);
  }

  public void close() {
    controller.close();
  }
}
