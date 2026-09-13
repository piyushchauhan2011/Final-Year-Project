package com.grocerykart.routing.io;

import com.grocerykart.routing.model.*;
import java.io.*;
import java.math.*;
import java.nio.file.*;
import java.util.*;

public final class DatasetLoader {
  public RoutingProblem load(Path demand, Path matrix)
      throws IOException, InfeasibleProblemException {
    try (var d = Files.newBufferedReader(demand);
        var m = Files.newBufferedReader(matrix)) {
      return load(demand.toString(), d, matrix.toString(), m);
    }
  }

  public RoutingProblem load(Reader demand, Reader matrix)
      throws IOException, InfeasibleProblemException {
    return load("demand", demand, "matrix", matrix);
  }

  private RoutingProblem load(
      String demandName, Reader demandReader, String matrixName, Reader matrixReader)
      throws IOException, InfeasibleProblemException {
    var demands = new LinkedHashMap<String, Integer>();
    var br = new BufferedReader(demandReader);
    String line;
    int row = 0;
    while ((line = br.readLine()) != null) {
      row++;
      var p = line.trim().split("\\s+");
      if (line.isBlank()) continue;
      if (p.length != 2)
        throw new DatasetFormatException(demandName, row, 1, "expected code and demand");
      int value;
      try {
        value = Integer.parseInt(p[1]);
      } catch (NumberFormatException e) {
        throw new DatasetFormatException(demandName, row, 2, "positive integer demand required");
      }
      if (value <= 0 || demands.putIfAbsent(p[0], value) != null)
        throw new DatasetFormatException(
            demandName, row, 1, "duplicate code or non-positive demand");
    }
    if (demands.isEmpty()) throw new DatasetFormatException(demandName, 1, 1, "no demands");
    br = new BufferedReader(matrixReader);
    line = br.readLine();
    if (line == null) throw new DatasetFormatException(matrixName, 1, 1, "missing header");
    var header = List.of(line.trim().split("\\s+"));
    if (header.size() < 2) throw new DatasetFormatException(matrixName, 1, 1, "missing columns");
    var labels = new ArrayList<>(header.subList(1, header.size()));
    if (new HashSet<>(labels).size() != labels.size() || !labels.contains("DC"))
      throw new DatasetFormatException(matrixName, 1, 1, "columns must be unique and include DC");
    int n = labels.size();
    long[][] raw = new long[n][n];
    var seen = new HashSet<String>();
    row = 1;
    while ((line = br.readLine()) != null) {
      row++;
      if (line.isBlank()) continue;
      var p = line.trim().split("\\s+");
      if (p.length != n + 1)
        throw new DatasetFormatException(matrixName, row, 1, "row width differs from header");
      String name = p[0];
      if (!labels.contains(name) || !seen.add(name))
        throw new DatasetFormatException(matrixName, row, 1, "unknown or duplicate row label");
      int i = labels.indexOf(name);
      for (int j = 0; j < n; j++) raw[i][j] = distance(matrixName, row, j + 2, p[j + 1], i == j);
    }
    if (!seen.equals(new HashSet<>(labels)))
      throw new DatasetFormatException(matrixName, row + 1, 1, "row labels differ from columns");
    var wanted = new HashSet<>(demands.keySet());
    var actual = new HashSet<>(labels);
    actual.remove("DC");
    if (!wanted.equals(actual))
      throw new DatasetFormatException(matrixName, 1, 1, "demand and matrix store codes differ");
    var stores = new ArrayList<Store>();
    int depot = labels.indexOf("DC");
    for (int i = 0; i < n; i++)
      stores.add(new Store(i, labels.get(i), i == depot ? 0 : demands.get(labels.get(i))));
    var problem =
        new RoutingProblem(stores, depot, new DistanceMatrix(raw), RoutingConstraints.DEFAULT);
    preflight(problem);
    return problem;
  }

  private static long distance(String file, int row, int col, String token, boolean diagonal)
      throws DatasetFormatException {
    if (diagonal && token.equals("-")) return 0;
    try {
      var value = new BigDecimal(token);
      if (value.scale() > 1 || value.signum() < 0 || (diagonal && value.signum() != 0))
        throw new NumberFormatException();
      return value.movePointRight(1).longValueExact();
    } catch (ArithmeticException | NumberFormatException e) {
      throw new DatasetFormatException(
          file,
          row,
          col,
          diagonal ? "diagonal must be '-' or zero" : "non-negative one-decimal distance required");
    }
  }

  private static void preflight(RoutingProblem p) throws InfeasibleProblemException {
    for (var s : p.stores())
      if (s.index() != p.depotIndex()) {
        long d =
            p.distances().distance(p.depotIndex(), s.index())
                + p.distances().distance(s.index(), p.depotIndex());
        long t = d * 9L + p.constraints().serviceSecondsPerStop();
        if (s.demandKg() > p.constraints().capacityKg()
            || d > p.constraints().maxDistanceDeciKm()
            || t > p.constraints().maxDurationSeconds())
          throw new InfeasibleProblemException("customer " + s.code() + " cannot be served alone");
      }
  }
}
