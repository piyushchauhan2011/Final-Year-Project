package com.grocerykart.routing;

import java.util.Arrays;
import java.util.random.RandomGenerator;

/** Standard partially mapped crossover for depot-free customer permutations. */
public final class PmxCrossover {
  public int[][] crossover(int[] first, int[] second, RandomGenerator random) {
    if (first.length != second.length)
      throw new IllegalArgumentException("parents differ in length");
    if (first.length < 2) return new int[][] {first.clone(), second.clone()};
    int from = random.nextInt(first.length);
    int to = random.nextInt(first.length - 1);
    if (to >= from) to++;
    if (from > to) {
      int swap = from;
      from = to;
      to = swap;
    }
    return new int[][] {child(first, second, from, to), child(second, first, from, to)};
  }

  static int[] child(int[] receivingParent, int[] segmentParent, int from, int to) {
    requireSamePermutation(receivingParent, segmentParent);
    int[] child = receivingParent.clone();
    System.arraycopy(segmentParent, from, child, from, to - from + 1);
    for (int position = 0; position < child.length; position++) {
      if (position >= from && position <= to) continue;
      int gene = receivingParent[position];
      int mappedPosition;
      while ((mappedPosition = indexOf(segmentParent, gene, from, to)) >= 0) {
        gene = receivingParent[mappedPosition];
      }
      child[position] = gene;
    }
    return child;
  }

  private static void requireSamePermutation(int[] first, int[] second) {
    int[] sortedFirst = first.clone();
    int[] sortedSecond = second.clone();
    Arrays.sort(sortedFirst);
    Arrays.sort(sortedSecond);
    if (!Arrays.equals(sortedFirst, sortedSecond)) {
      throw new IllegalArgumentException("parents must contain the same customers exactly once");
    }
  }

  private static int indexOf(int[] values, int value, int from, int to) {
    for (int index = from; index <= to; index++) {
      if (values[index] == value) return index;
    }
    return -1;
  }
}
