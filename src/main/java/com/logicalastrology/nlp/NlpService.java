package com.logicalastrology.nlp;

import org.springframework.stereotype.Service;

import java.util.List;

/*
  Placeholder for NLP operations: tokenization, embeddings, summarization.
  Recommended approach: implement embeddings via external service (Python microservice or HF Inference API).
*/
@Service
public class NlpService {

    public double calculateCoherence(List<String> texts) {
        // naive placeholder: returns 0.0
        return 0.0;
    }

    public String summarize(List<String> texts) {
        return String.join(" ", texts).substring(0, Math.min(200, String.join(" ", texts).length()));
    }
}
