package com.grocerykart.report;

import com.grocerykart.routing.model.RoutingSolution;
import java.math.*;

public final class CostCalculator {
  public record Costs(
      BigDecimal dailyVariable,
      BigDecimal monthlyMaintenance,
      BigDecimal annualVariable,
      BigDecimal threeYearOperating) {}

  public static Costs calculate(RoutingSolution s) {
    var daily =
        BigDecimal.valueOf(s.totalDistanceDeciKm())
            .movePointLeft(1)
            .multiply(BigDecimal.valueOf(30));
    var maintenance = BigDecimal.valueOf(s.routes().size()).multiply(BigDecimal.valueOf(20000));
    return new Costs(
        daily,
        maintenance,
        daily.multiply(BigDecimal.valueOf(365)),
        maintenance.multiply(BigDecimal.valueOf(36)).add(daily.multiply(BigDecimal.valueOf(1095))));
  }
}
