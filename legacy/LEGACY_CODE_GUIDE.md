# Legacy GroceryKart Code Guide

> **Historical reference only.** This document describes the original Java 7, NetBeans, Swing, and iText 5 application. The snippets below preserve its important genetic-algorithm ideas and the defects that motivated the Java 21 replacement. They are not examples to copy into production code.

The full original source remains available in Git revision [`c00e688`](https://github.com/piyushchauhan2011/Final-Year-Project/tree/c00e68813a4c0492ce933d7802d0b9888d9131e6/GroceryKart/src/grocerykart). A self-contained compiled copy is under [`legacy/GroceryKart/`](GroceryKart/).

## Original structure

| Legacy class | Responsibility | Modern replacement |
|---|---|---|
| `authenticate` | Swing login with hard-coded demonstration credentials | Removed; no identity backend exists |
| `GroceryKartForm` | Swing form and direct, synchronous GA execution | `app/MainView` and `app/RoutingController` |
| `GroceryKart` inside `GroceryKartForm.java` | Population generation, route splitting, ranking, PMX, BCRC, mutation, and persistence | `routing/GeneticAlgorithmRouter`, crossover classes, decoder, archive, and validator |
| `ReadData` | Demand and distance parsing | `routing/io/DatasetLoader` |
| `Candidate` and `Route` | Mutable chromosome and route representation | Immutable records under `routing/model` plus private GA candidates |
| `ReportGeneration` | iText report generated from serialized results | `report/PdfReportExporter` using PDFBox |
| `GroceryKartSavedData` and analysis frames | Read serialized historical candidates and display costs | In-memory `ComparisonResult`, JavaFX tabs, and `CostCalculator` |

## Candidate and route representation

The original model captured the central GA idea: a chromosome represented a giant tour, and `routeTable` held its truck routes.

```java
class Route implements Serializable {
    public ArrayList<Integer> route;
    public double routeFitness;
}

class Candidate implements Comparable<Candidate>, Serializable {
    ArrayList<Integer> chromosome;
    double fitness;
    int rank;
    ArrayList<Route> routeTable;
}
```

This concept survives in the modern implementation, but mutable lists and serialized candidates do not. A private genome is decoded into immutable `RoutingSolution` and `VehicleRoute` values, then checked by `SolutionValidator`.

The original ordering implementation was invalid:

```java
@Override
public int compareTo(Candidate tmp) {
    if (this.fitness < tmp.fitness) {
        return 1;
    } else if (this.fitness == tmp.fitness) {
        return -1;
    } else {
        return 0;
    }
}
```

It returns `-1` for equal values and `0` for unequal greater values, violating the `Comparable` contract. Sorting could therefore be unstable or incorrect. The modern code uses explicit objective comparators and solution signatures.

## Data-loading identity defect

`ReadData.readDemandTable` inserted `DC` at index `0` before reading stores:

```java
tmp = new Store();
tmp.storeName = "DC";
tmp.storeId = 0;
tmp.demand = 0;
g.stores.add(tmp);

while (in.hasNext()) {
    tmp = new Store();
    tmp.storeName = in.next();
    tmp.storeId = k;
    tmp.demand = Integer.parseInt(in.next());
    g.stores.add(tmp);
}
```

The matrix reader did not reorder the labelled matrix to match that synthetic index. It scanned to the `DC` header and then copied rows positionally:

```java
String tmp = in.next();
while (tmp.compareTo("DC") != 0) {
    tmp = in.next();
}
g.DCStoreMatrix = new double[g.storeSize][g.storeSize];
for (int i = 0; i < g.storeSize; i++) {
    in.next();
    for (int j = 0; j < g.storeSize; j++) {
        tmp = in.next();
        g.DCStoreMatrix[i][j] = tmp.equals("-") ? -1 : Double.parseDouble(tmp);
    }
}
```

In the bundled matrix, `DC` is the final node. Consequently, domain index `0` meant `DC` in one structure but `ST0103` in the matrix. Exceptions were swallowed, so malformed or incomplete input could proceed silently.

`DatasetLoader` now matches demand, row, and column labels by identity; resolves the sole `DC`; scales decimal kilometres exactly; rejects malformed data; and preflights every customer.

## Route-splitting boundary defect

The original `generateRoutes` attempted to split the chromosome whenever capacity, stops, or half-distance was exceeded:

```java
for (int i = 1; i < length; i++) {
    currentStops += 1;
    currentDemand += stores.get(tmp.chromosome.get(i)).demand;
    currentDistance += DCStoreMatrix
        [tmp.chromosome.get(i - 1)]
        [tmp.chromosome.get(i)];

    if (currentDemand <= maxDemand
            && currentStops <= maxStops
            && currentDistance <= maxDistanceTravelled / 2) {
        temp.route.add(tmp.chromosome.get(i));
    } else {
        currentDemand = 0;
        currentStops = 0;
        currentDistance = 0;
        alroutesParent.add(temp);
        temp = new Route();
        temp.route.add(tmp.chromosome.get(0));
    }
}
```

The customer that triggered `else` was not added to the completed route or the new route. The route reset therefore dropped boundary customers. The half-distance heuristic also did not evaluate the actual completed depot-to-depot route.

`GiantTourDecoder` replaces this with dynamic programming over every feasible contiguous segment. Every segment is explicitly depot-framed, every triggering customer remains in the permutation, and decoding fails rather than dropping a customer.

## Distance-scoring defect

The historical route fitness summed only arcs already present in the mutable route:

```java
private void evaluateFitnessRoute(Route tmp) {
    double tmpfitness = 0;
    for (int i = 1; i < tmp.route.size(); i++) {
        int p = tmp.route.get(i - 1);
        int q = tmp.route.get(i);
        if (DCStoreMatrix[p][q] != 0) {
            tmpfitness += DCStoreMatrix[p][q];
        }
    }
    tmp.routeFitness = tmpfitness;
}
```

Routes were not consistently framed with a return depot, so the final store-to-warehouse leg was omitted. Service time and completed-route duration were also absent from this calculation.

`RouteEvaluator` is now the single metric implementation. It includes departure, inter-store arcs, return travel, load, stop count, travel seconds, service seconds, and total duration.

## Preserved mutation idea

The project documented inversion mutation over a short segment. Its original probability check and remove-while-iterating behavior were incorrect:

```java
private void mutation(ArrayList<Candidate> population) {
    Random r = new Random();
    int popSize = population.size();
    for (int i = 0; i < popSize; i++) {
        if (r.nextDouble() > GroceryKart.mutationProbability) {
            Candidate temp = inversion(population.remove(i));
            population.add(temp);
        }
    }
}
```

Using `>` reverses the configured probability: a mutation rate of `0.10` mutates about 90% of candidates. Removing and appending during indexed iteration also changes which candidates are visited.

The modern GA keeps the short inversion concept but mutates an offspring only when `random.nextDouble() < mutationRate`. It reverses an array segment without changing permutation membership.

## Preserved PMX concept

The original PMX implementation selected two parents and two distinct cut points, copied the opposite segment, and followed replacement mappings outside the segment. This verbatim excerpt preserves the central operation:

```java
int cuttingPoint1 = r.nextInt(chromosomeSize);
int cuttingPoint2 = r.nextInt(chromosomeSize);
while (cuttingPoint1 == cuttingPoint2) {
    cuttingPoint2 = r.nextInt(chromosomeSize);
}

for (int i = cuttingPoint1; i <= cuttingPoint2; i++) {
    offspring1Vector.remove(i);
    offspring1Vector.add(i, copyParent2.chromosome.get(i));
    offspring2Vector.remove(i);
    offspring2Vector.add(i, copyParent1.chromosome.get(i));

    int index = copyParent2.chromosome.get(i);
    replacement1.remove(index);
    replacement1.add(index, copyParent1.chromosome.get(i));
    index = copyParent1.chromosome.get(i);
    replacement2.remove(index);
    replacement2.add(index, copyParent2.chromosome.get(i));
}
```

The implementation mixed list indices and customer values and used value-based `remove(Integer)` calls, which could produce invalid offspring. `PmxCrossover` now implements the same documented operator over depot-free arrays and verifies that both parents contain the same customer set.

## Preserved BCRC concept

The original Best Cost Route Crossover selected one route from each parent, removed its customers from the other parent, and considered feasible insertion positions. The following excerpts are verbatim; unrelated debug comments are omitted between blocks:

```java
int tmp = r.nextInt(copyParent1.routeTable.size());
Route keyParent1 = new Route();
for (int i : copyParent1.routeTable.get(tmp).route) {
    keyParent1.route.add(i);
}

tmp = r.nextInt(copyParent2.routeTable.size());
Route keyParent2 = new Route();
for (int i : copyParent2.routeTable.get(tmp).route) {
    keyParent2.route.add(i);
}

ArrayList<Route> newalroutesParent2 =
    removeSelectedElements(keyParent1, copyParent2.routeTable);
ArrayList<Route> newalroutesParent1 =
    removeSelectedElements(keyParent2, copyParent1.routeTable);

for (int i : keyParent1.route) {
    if (i == 0) {
        continue;
    }
    ArrayList<Candidate> tmpCandidates = new ArrayList<Candidate>();
    for (int k = 0; k < newalroutesParent2.size(); k++) {
        Route jroute = newalroutesParent2.get(k);
        for (int j = 1; j < jroute.route.size(); j++) {
            Candidate tmpCopyCandidate = copyCandidate(copyParent2);
            tmpCopyCandidate.routeTable.get(k).route.add(j, i);
            if (checkInsertionValidity(tmpCopyCandidate.routeTable.get(k))) {
                tmpCopyCandidate.fitness =
                    evaluateFitnessCandidate(tmpCopyCandidate);
                tmpCandidates.add(tmpCopyCandidate);
            }
        }
    }
    Collections.sort(tmpCandidates);
    if (tmpCandidates.size() != 0) {
        copyParent2 = tmpCandidates.get(0);
    }
}
```

That route-based recombination idea is retained in `BestCostRouteCrossover`. The modern operator removes donor customers, tries every feasible edge using full directed depot-to-depot metrics, uses deterministic tie order, and creates a singleton route when no existing insertion is feasible.

The old feasibility check illustrates why centralized evaluation was required:

```java
private boolean checkInsertionValidity(Route tmp) {
    if (tmp.route.size() > maxStops) return false;

    double currentDistance = DCStoreMatrix[0][tmp.route.get(0)];
    int currentDemand = stores.get(tmp.route.get(0)).demand;
    for (int i = 1; i < tmp.route.size(); i++) {
        currentDistance += DCStoreMatrix
            [tmp.route.get(i - 1)]
            [tmp.route.get(i)];
        currentDemand += stores.get(tmp.route.get(i)).demand;
    }

    return currentDemand <= maxDemand
        && currentDistance <= maxDistanceTravelled / 2;
}
```

It omitted return distance, stop service time, and route duration and depended on the incorrect index-0 depot assumption.

## Pareto-selection intent

The original code intended to optimize two objectives—truck count and distance—and group non-dominated candidates into ranks. It destructively removed candidates while ranking and later appended ranked candidates back into the active population. Tournament fitness also indexed `currentCandidates` with the sampled-list position instead of the sampled candidate ID in some paths.

The modernization keeps the multi-objective intent while using immutable validated solutions, bounded non-dominated archives, deterministic signatures, and fixed-size populations. Equal-metric but structurally different route sets can remain separate alternatives.

## Historical report and persistence

`ReportGeneration` used iText 5 and read Java-serialized candidates:

```java
list.add(new ListItem("Quaterly Maintainance cost: "
    + gr.getMaintenanceCost(can, 3)));
list.add(new ListItem("Yearly Variable cost: "
    + gr.getVariableCost(can, 365)));
list.add(new ListItem("No. of trucks to buy: "
    + gr.getTotalTrucks(can)));
list.add(new ListItem("Total distance travelled by trucks: "
    + gr.getTotalDistance(can)));
```

Those stored candidates inherited the identity and metric defects above. The old serialized `.kart` files and generated report are therefore not preserved as trustworthy results. The modern application exports only the currently selected, independently validated alternative through PDFBox.

## Retrieving the complete original source

The archived JAR is runnable but intentionally does not duplicate the entire NetBeans source tree. Retrieve any original source file directly from the preserved Git revision:

```sh
git show c00e68813a4c0492ce933d7802d0b9888d9131e6:GroceryKart/src/grocerykart/ReadData.java
git show c00e68813a4c0492ce933d7802d0b9888d9131e6:GroceryKart/src/grocerykart/Candidate.java
git show c00e68813a4c0492ce933d7802d0b9888d9131e6:GroceryKart/src/grocerykart/GroceryKartForm.java
git show c00e68813a4c0492ce933d7802d0b9888d9131e6:GroceryKart/src/grocerykart/ReportGeneration.java
```

This keeps the repository's active source tree unambiguous while preserving both the original executable and the historically significant implementation details.
