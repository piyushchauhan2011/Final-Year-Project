package com.grocerykart.routing.model;

import java.util.Arrays;

public final class DistanceMatrix {
  private final long[][] values;

  public DistanceMatrix(long[][] values) {
    if (values == null || values.length == 0) throw new IllegalArgumentException("empty matrix");
    this.values =
        Arrays.stream(values)
            .map(
                row -> {
                  if (row == null || row.length != values.length)
                    throw new IllegalArgumentException("matrix must be square");
                  return row.clone();
                })
            .toArray(long[][]::new);
  }

  public int size() {
    return values.length;
  }

  public long distance(int from, int to) {
    return values[from][to];
  }
}
