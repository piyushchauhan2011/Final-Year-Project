package com.grocerykart.routing;

import static org.junit.jupiter.api.Assertions.*;

import com.grocerykart.routing.io.DatasetLoader;
import com.grocerykart.routing.model.*;
import java.io.StringReader;
import java.time.Duration;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class DatasetLoaderSmokeTest {
  @Test
  void resolvesLastDepotByLabelAndIncludesReturnLeg() throws Exception {
    var p =
        new DatasetLoader()
            .load(
                new StringReader("A 5\nB 6\n"),
                new StringReader("Store A B DC\nA - 1.0 2.0\nB 1.0 - 3.0\nDC 2.0 3.0 -\n"));
    assertEquals(2, p.depotIndex());
    assertEquals(20, p.distances().distance(2, 0));
    var solution =
        GiantTourDecoder.decode(
            new int[] {0, 1}, p, Algorithm.GA_PMX, Duration.ZERO, OptionalLong.of(42));
    assertEquals(60, solution.totalDistanceDeciKm());
    assertTrue(SolutionValidator.validate(p, solution).valid());
  }
}
