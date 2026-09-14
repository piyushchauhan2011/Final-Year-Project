MAVEN ?= mvn

.PHONY: run test verify format format-check clean

run:
	$(MAVEN) javafx:run

test:
	$(MAVEN) spotless:check test

verify:
	$(MAVEN) -Dtest=BundledComparisonIT test

format:
	$(MAVEN) spotless:apply

format-check:
	$(MAVEN) spotless:check

clean:
	$(MAVEN) clean
