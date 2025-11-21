package com.logicalastrology.nlp;

import java.util.Collections;
import java.util.List;

public record AiAnalysisResult(String summary,
                               String sentiment,
                               double coherenceScore,
                               boolean generated,
                               List<String> highlights) {

    public AiAnalysisResult {
        highlights = highlights == null ? Collections.emptyList() : List.copyOf(highlights);
    }
}
