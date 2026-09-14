package com.grocerykart.routing.model;

import java.time.Duration;
import java.util.OptionalLong;

public record SolutionProvenance(Algorithm algorithm, OptionalLong seed, Duration foundAt) {
  public SolutionProvenance {
    seed = seed == null ? OptionalLong.empty() : seed;
  }
}
