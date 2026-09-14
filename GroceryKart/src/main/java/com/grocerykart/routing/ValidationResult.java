package com.grocerykart.routing;

import java.util.List;

public record ValidationResult(boolean valid, List<String> errors) {
  public ValidationResult {
    errors = List.copyOf(errors);
  }

  public static ValidationResult ok() {
    return new ValidationResult(true, List.of());
  }
}
