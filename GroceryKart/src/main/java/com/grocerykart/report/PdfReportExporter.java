package com.grocerykart.report;

import com.grocerykart.routing.SolutionValidator;
import com.grocerykart.routing.model.*;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;

public final class PdfReportExporter {
  public void export(ComparisonResult comparison, Alternative selected, Path output)
      throws IOException {
    if (selected == null
        || !SolutionValidator.validate(comparison.problem(), selected.representative()).valid())
      throw new IllegalArgumentException("select a validated alternative");
    try (var document = new PDDocument()) {
      try (var writer = new ReportWriter(document)) {
        writer.line("GroceryKart routing comparison");
        writer.line("Solver runs");
        for (var run : comparison.runs())
          writer.line(
              run.algorithm()
                  + ": "
                  + run.terminalStatus()
                  + ", elapsed "
                  + run.elapsed()
                  + ", candidates "
                  + run.candidateEvaluations());
        writer.line("Alternatives: " + comparison.alternatives().size());
        for (var alternative : comparison.alternatives())
          writer.line(
              alternative.representative().algorithm()
                  + ": "
                  + alternative.representative().routes().size()
                  + " vehicles, "
                  + alternative.representative().totalDistanceDeciKm()
                  + " deci-km");
        var solution = selected.representative();
        writer.line("Selected solution: " + solution.algorithm());
        writer.line("Number of trucks needed: " + solution.routes().size());
        writer.line(
            "Total distance travelled each day: "
                + kilometres(solution.totalDistanceDeciKm())
                + " km");
        writer.line("Selected provenance: " + selected.provenance());
        var costs = CostCalculator.calculate(solution);
        writer.line("Daily variable cost (Rs.): " + costs.dailyVariable().toPlainString());
        writer.line(
            "Monthly fleet maintenance (Rs.): " + costs.monthlyMaintenance().toPlainString());
        writer.line("Annual variable cost (Rs.): " + costs.annualVariable().toPlainString());
        writer.line(
            "Total operating cost for first three years (Rs.): "
                + costs.threeYearOperating().toPlainString());
        writer.line(
            "Feasibility: independently validated; all trucks return to DC and satisfy the 20-stop, 1000-kg, 65.0-km, and 5:00-8:00 limits.");
        writer.line("Routing for each truck");
        for (var route : solution.routes()) {
          writer.line(
              "Truck "
                  + route.vehicleNumber()
                  + " - load "
                  + route.loadKg()
                  + " kg; stops "
                  + route.customerCount()
                  + "; completed distance "
                  + kilometres(route.distanceDeciKm())
                  + " km; travel/service/total "
                  + route.travelSeconds()
                  + "/"
                  + route.serviceSeconds()
                  + "/"
                  + route.durationSeconds()
                  + " seconds");
          writer.line(
              "Route: "
                  + route.nodeIndices().stream()
                      .map(node -> comparison.problem().stores().get(node).code())
                      .collect(java.util.stream.Collectors.joining(" -> ")));
        }
      }
      document.save(output.toFile());
    }
  }

  private static String kilometres(long deciKilometres) {
    return String.format(java.util.Locale.ROOT, "%.1f", deciKilometres / 10.0);
  }

  private static final class ReportWriter implements AutoCloseable {
    private final PDDocument document;
    private PDPageContentStream stream;
    private float y;

    ReportWriter(PDDocument document) throws IOException {
      this.document = document;
      nextPage();
    }

    void line(String text) throws IOException {
      String clean = text.replaceAll("[^\\x20-\\x7e]", "?");
      while (clean.length() > 105) {
        int split = clean.lastIndexOf(' ', 105);
        if (split < 1) split = 105;
        physicalLine(clean.substring(0, split));
        clean = "  " + clean.substring(split).stripLeading();
      }
      physicalLine(clean);
    }

    private void physicalLine(String text) throws IOException {
      if (y < 54) nextPage();
      stream.showText(text);
      stream.newLineAtOffset(0, -13);
      y -= 13;
    }

    private void nextPage() throws IOException {
      if (stream != null) {
        stream.endText();
        stream.close();
      }
      var page = new PDPage();
      document.addPage(page);
      stream = new PDPageContentStream(document, page);
      stream.beginText();
      stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
      stream.newLineAtOffset(48, 742);
      y = 742;
    }

    @Override
    public void close() throws IOException {
      if (stream != null) {
        stream.endText();
        stream.close();
      }
    }
  }
}
