GROCERYKART LEGACY APPLICATION
==============================

ARCHIVAL SOFTWARE — NOT THE CURRENT VERIFIED IMPLEMENTATION

This directory contains the original Java 7 / NetBeans / Swing application as a
standalone runnable bundle. It is retained only to reproduce and inspect the
historical project. Its routing results are known to be incorrect: the original
loader shifted depot/store identities, route splitting could omit customers, and
route totals omitted return-to-warehouse travel and time. Do not use its output
for operational decisions or compare its totals with the modern application.

Contents
--------

GroceryKart.jar              Original runnable application
lib/itextpdf-5.4.0.jar       Original PDF dependency
DemandTable.txt              Original 600-store demand input
DistanceMatrix.txt           Original 601-node distance input

Run
---

Open a terminal in this directory and run:

    java -jar GroceryKart.jar

The JAR manifest selects grocerykart.authenticate and references the bundled
lib/itextpdf-5.4.0.jar. The original UI may ask you to choose DemandTable.txt and
DistanceMatrix.txt; select the copies in this directory.

Historical login
----------------

The original application contains hard-coded demonstration credentials:

    Username: samicheen
    Password: khariwal

Compatibility
-------------

The JAR was compiled with Java 7. It has been launch-smoke-tested from this
self-contained directory using the current Java runtime, but old Swing behavior
may differ by operating system. The modern Java 21 application is under
../../GroceryKart and should be used for corrected routing and reporting.

Legacy code guide
-----------------

See ../LEGACY_CODE_GUIDE.md for annotated excerpts of the original loader,
candidate model, route splitting, fitness scoring, mutation, PMX, BCRC, Pareto
ranking, and report generation. The complete source is preserved in the Git
revision linked from that guide.
