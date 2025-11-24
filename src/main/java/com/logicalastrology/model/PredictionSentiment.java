package com.logicalastrology.model;

public enum PredictionSentiment {
    POSITIVO,
    NEGATIVO;

    public static PredictionSentiment fromString(String value) {
        if (value == null) {
            return null;
        }
        return PredictionSentiment.valueOf(value.trim().toUpperCase());
    }
}
