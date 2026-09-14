package com.grocerykart.routing.model;

import java.time.Duration;

public record ProgressPoint(
    Duration elapsed,
    int vehicleCount,
    long totalDistanceDeciKm,
    long candidateEvaluations,
    long completedGenerations) {}
