# Logical Astrology - Java (Spring Boot) skeleton

This repository is a project skeleton for **Logical Astrology** backend implemented in Java with Spring Boot.

## Features included
- Spring Boot application starter
- Example JPA entities for horoscopes and analysis
- Jsoup-based sample scraper component
- Scheduled job skeleton that runs daily
- REST controllers for fetching horoscopes and analysis
- AI-ready NLP service that can call OpenAI-compatible endpoints (with graceful fallback)
- Maven build (pom.xml) and Dockerfile

## How to build
1. Install Java 21 and Maven.
2. Configure `src/main/resources/application.properties` for your PostgreSQL (or adjust to H2) and, optionally, the `ai.*` properties if you want to enable an LLM for the consolidated analysis endpoint.
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

## AI configuration

The `/api/analise/{signo}` endpoint now uses the `NlpService` to talk to OpenAI-compatible chat completion APIs. To enable it:

1. Provide an API key via `OPENAI_API_KEY` (or set `ai.api-key` directly).
2. Toggle `ai.enabled=true` and adjust `ai.endpoint`, `ai.model`, and `ai.temperature` if you are using a different provider (for example, an on-premise gateway or Hugging Face endpoint that mimics the OpenAI schema).
3. If the service is disabled or the request fails, the application automatically falls back to a deterministic statistical summary so the endpoint never breaks.
