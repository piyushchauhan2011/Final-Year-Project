package com.grocerykart.routing.model;

public record Store(int index, String code, int demandKg) {
  public Store {
    if (index < 0 || code == null || code.isBlank() || demandKg < 0)
      throw new IllegalArgumentException("invalid store");
  }
}
