# ScaleUp: Order Processing & Performance Lab

A hands-on project to learn **Apache Kafka** and **Redis** by building the same
e-commerce order processing system three ways, reproducing real-world problems,
and measuring the results.

## Implementations

| Folder | Phase | Description | Status |
|--------|-------|-------------|--------|
| `scaleup-normal` | 1 | Spring Boot + MySQL (baseline, no Kafka/Redis) | In progress |
| `scaleup-kafka`  | 2 | Spring Boot + MySQL + Apache Kafka | Not started |
| `scaleup-redis`  | 3 | Spring Boot + MySQL + Redis | Not started |

## Other Folders

- `documentation/`: problems reproduced, solutions, screenshots, lessons learned
- `performance-results/`: measured results (response time, P95, RPS, error rate)
- `docker/`: Docker Compose files for infrastructure (added when needed)

## Tech Stack (Phase 1)

Java 21, Spring Boot 3.x, Maven, MySQL 8

## Principles

- Each implementation is an independent Maven project.
- All performance claims are backed by actual measurements.
- No secrets are committed to this repository.