package com.grocerykart.routing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grocerykart.routing.io.DatasetLoader;
import java.io.StringReader;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class OrToolsVehicleRouterSmokeTest {
  @Test
  void solvesAndValidatesEveryMandatoryCustomer() throws Exception {
    var problem =
        new DatasetLoader()
            .load(
                new StringReader("A 5\nB 6\n"),
                new StringReader(
                    "Store A B DC\n" + "A - 1.0 2.0\n" + "B 1.0 - 3.0\n" + "DC 2.0 3.0 -\n"));
    var run =
        new OrToolsVehicleRouter()
            .run(
                problem,
                new ComparisonConfig(Duration.ofSeconds(5), 20, 0.9, 0.1, 42, 10),
                ignored -> {});
    assertFalse(run.retainedSolutions().isEmpty());
    assertTrue(SolutionValidator.validate(problem, run.retainedSolutions().getFirst()).valid());
  }
}
