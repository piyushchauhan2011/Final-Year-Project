# GroceryKart Routing Optimizer

GroceryKart is a Java 21 and JavaFX application for planning daily warehouse-to-store deliveries across a 600-store network. It compares two seeded genetic-algorithm variants with Google OR-Tools, independently validates every retained solution, and presents feasible alternatives ranked by estimated three-year operating cost.

The original academic project implements the MUPHORIA transportation challenge. The historical project report is retained in [`SUPER FINAL.pdf`](SUPER%20FINAL.pdf).

## Problem constraints

Every generated truck route:

- starts at the central warehouse (`DC`);
- visits no more than 20 stores;
- carries no more than 1,000 kg;
- travels no more than 65.0 km;
- spends five minutes unloading at each store;
- uses an average travel time of 90 seconds per kilometre;
- completes within the configured three-hour operating window; and
- returns to `DC`.

Every store is mandatory and must be visited exactly once across the complete solution. The current implementation applies the three-hour limit to the complete depot-to-depot route, including return travel.

## Solvers

The comparison runs these solvers sequentially so they do not compete for CPU:

1. **Genetic Algorithm (PMX)** — permutation genomes with partially mapped crossover.
2. **Genetic Algorithm (BCRC)** — route removal followed by best-cost feasible reinsertion.
3. **Google OR-Tools** — constrained vehicle-routing search using guided local search.

Each solver receives the same time budget. The default is 60 seconds per solver, so a complete comparison normally takes about three minutes plus dataset loading and native-library startup. The UI displays the active solver, elapsed time, overall progress, and valid improvements while the comparison runs.

All solvers are time-limited heuristics. A displayed solution is proven feasible by the independent validator, but it is not necessarily the globally shortest or globally cheapest solution.

## Cost model

Costs are calculated from the selected complete solution:

- daily variable cost: `distance in km × Rs. 30`;
- monthly fleet maintenance: `truck count × Rs. 20,000`;
- annual variable cost: `daily variable cost × 365`; and
- three-year operating cost: `monthly maintenance × 36 + daily variable cost × 1,095`.

The application preserves non-dominated truck-count/distance alternatives and lists them by estimated three-year operating cost.

## Requirements

- JDK 21 with JavaFX support
- Maven 3.9 or newer
- GNU Make or BSD Make (optional; Maven commands can be run directly)

The project has been verified with Azul Zulu JavaFX 21.0.11 and Maven 3.9.16 on Apple Silicon macOS. Maven downloads JavaFX, OR-Tools, PDFBox, JUnit, and native dependencies automatically.

Check the active toolchain:

```sh
java -version
mvn -version
```

## Run the application

From the repository root:

```sh
cd GroceryKart
make run
```

Equivalent Maven command:

```sh
mvn javafx:run
```

In the application:

1. Keep the default solver settings or choose a per-solver time budget from 5 to 600 seconds.
2. Click **Run Comparison**.
3. Follow the progress bar and current-solver status.
4. Review the solver summaries when all three runs finish.
5. Select an alternative to inspect its truck count, daily kilometres, cost, routes, and store assignments.
6. Click **Export selected PDF** to save the complete comparison and selected routing plan.

Close the JavaFX window normally to stop the background executor and exit the application.

## Output

For each selected solution, the UI and PDF report include:

- number of trucks required;
- total kilometres travelled each day;
- complete `DC -> stores -> DC` routing for every truck;
- load, stop count, distance, and duration for each route;
- store-to-truck assignment and visit order;
- solver provenance and status; and
- daily, monthly, annual, and three-year cost metrics.

Distances are reported in kilometres because the input matrix, route constraint, travel-time rate, and variable cost are all kilometre-based.

## Bundled data

The authoritative dataset is packaged with the application:

```text
GroceryKart/src/main/resources/data/DemandTable.txt
GroceryKart/src/main/resources/data/DistanceMatrix.txt
```

The demand file contains one unique positive demand per store:

```text
STORE_CODE DEMAND_KG
```

The matrix is a square, labelled distance matrix with one `DC` row and column. Distances may have at most one decimal place and are converted exactly to integer deci-kilometres internally. Row and column labels are matched by identity rather than physical order.

Dataset loading rejects duplicate labels, mismatched store sets, invalid diagonal values, negative/non-finite distances, and customers that cannot be served even by a single truck.

## Development commands

Run commands from `GroceryKart`:

```sh
make run          # launch the JavaFX application
make test         # check formatting and run unit/native smoke tests
make verify       # run all three solvers against the bundled 600-store dataset
make format       # apply Google Java Format
make format-check # check formatting without modifying files
make clean        # remove Maven build output
```

Direct Maven equivalents:

```sh
mvn spotless:check test
mvn -Dtest=BundledComparisonIT test
mvn spotless:apply
mvn clean
```

`make verify` uses a five-second budget for each solver and validates complete coverage, all route constraints, solver progress events, cost ordering, and generated PDF text. Results vary with available computation and time limits; the GA runs use seed `42` by default.

## Project layout

```text
GroceryKart/
├── pom.xml
├── Makefile
└── src/
    ├── main/
    │   ├── java/com/grocerykart/
    │   │   ├── app/          JavaFX application and controller
    │   │   ├── application/  sequential comparison orchestration
    │   │   ├── report/       costs and PDF export
    │   │   └── routing/      models, loading, validation, GA, and OR-Tools
    │   └── resources/
    │       ├── data/         bundled demand and distance inputs
    │       └── styles/       JavaFX stylesheet
    └── test/java/            unit, native, and bundled verification tests
```

## Historical material

- [`SUPER FINAL.pdf`](SUPER%20FINAL.pdf) — original project report
- [`project.pptx`](project.pptx) — original presentation

Legacy NetBeans, Ant, Swing, iText 5, serialized solution files, generated binaries, and invalid historical result artifacts have been removed from the runnable project.
