package com.grocerykart.routing;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Arrays;
import java.util.random.RandomGeneratorFactory;
import org.junit.jupiter.api.Test;

class PmxCrossoverTest {
  @Test
  void everyChildContainsEveryCustomerExactlyOnce() {
    int[] first = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    int[] second = {4, 5, 2, 1, 8, 7, 6, 9, 3};
    var random = RandomGeneratorFactory.of("L64X128MixRandom").create(42);
    for (int attempt = 0; attempt < 100; attempt++) {
      for (int[] child : new PmxCrossover().crossover(first, second, random)) {
        int[] sorted = child.clone();
        Arrays.sort(sorted);
        assertArrayEquals(first, sorted);
      }
    }
  }
}
