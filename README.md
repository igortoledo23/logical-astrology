# Logical Astrology - Java (Spring Boot) skeleton

This repository is a project skeleton for **Logical Astrology** backend implemented in Java with Spring Boot.

## Features included
- Spring Boot application starter
- Example JPA entities for horoscopes and analysis
- Jsoup-based sample scraper component
- Scheduled job skeleton that runs daily
- REST controllers for fetching horoscopes and analysis
- Placeholder NLP service for later integration
- Maven build (pom.xml) and Dockerfile

## How to build
1. Install Java 21 and Maven.
2. Configure `src/main/resources/application.properties` for your PostgreSQL (or adjust to H2).
3. Build:
```bash
mvn -U -DskipTests package
```
4. Run:
```bash
java -jar target/logical-astrology-0.0.1-SNAPSHOT.jar
```

## Notes
- Scraper selectors are placeholders and must be adapted for each source.
- For advanced NLP use embeddings (via Hugging Face, Sentence Transformers) — consider a Python microservice for that.
