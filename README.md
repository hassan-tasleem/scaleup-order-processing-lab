# ScaleUp: Order Processing & Performance Lab

A hands-on project to understand backend problems by reproducing them, measuring them,
and testing what Apache Kafka and Redis do (and do not) fix. We build the same e-commerce
order processing system three ways and run identical tests on each.

## Implementations

| Folder | Stack | Role | Status |
|--------|-------|------|--------|
| `scaleup-normal` | Spring Boot + PostgreSQL | Baseline: every problem is reproduced here first | In progress (Wave 0) |
| `scaleup-kafka` | Spring Boot + PostgreSQL + Apache Kafka | Same tests with asynchronous messaging | Not started |
| `scaleup-kafka-redis` | Spring Boot + PostgreSQL + Kafka + Redis | Same tests with caching and fast counters added | Not started |

## Other Folders

- `documentation/`: problem catalog, test strategy, one document per experiment, comparisons, conclusions
- `performance-results/`: raw and summarized measurements
- `jmeter/`: JMeter test plans and test data shared by all implementations
- `scripts/`: PowerShell helper scripts shared by all implementations
- `docker/`: Docker Compose files for infrastructure

## Tech Stack

Java 21, Spring Boot 3.5, Maven, PostgreSQL

## Principles

- Each implementation is an independent Maven project.
- The same workload and test conditions are used for every implementation.
- Every performance claim is backed by a recorded measurement. Expected results are
  written as hypotheses, never as results.
- No secrets are committed to this repository.