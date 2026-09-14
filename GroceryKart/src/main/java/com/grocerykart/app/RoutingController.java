package com.grocerykart.app;

import com.grocerykart.application.ComparisonProgressListener;
import com.grocerykart.application.ComparisonService;
import com.grocerykart.routing.*;
import com.grocerykart.routing.io.*;
import com.grocerykart.routing.model.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public final class RoutingController implements AutoCloseable {
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public CompletableFuture<ComparisonResult> compareBundled(
      ComparisonConfig config, ComparisonProgressListener progressListener) {
    return CompletableFuture.supplyAsync(
        () -> {
          try (var d = getClass().getResourceAsStream("/data/DemandTable.txt");
              var m = getClass().getResourceAsStream("/data/DistanceMatrix.txt")) {
            if (d == null || m == null) throw new IOException("bundled data unavailable");
            var p =
                new DatasetLoader()
                    .load(
                        new InputStreamReader(d, StandardCharsets.UTF_8),
                        new InputStreamReader(m, StandardCharsets.UTF_8));
            return new ComparisonService().compare(p, config, progressListener);
          } catch (IOException | InfeasibleProblemException e) {
            throw new CompletionException(e);
          }
        },
        executor);
  }

  public void close() {
    executor.shutdownNow();
  }
}
