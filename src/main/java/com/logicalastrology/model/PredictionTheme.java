package com.logicalastrology.model;

public enum PredictionTheme {
    AMOR,
    TRABALHO,
    FAMILIA,
    AMIGOS;

    public static PredictionTheme fromString(String value) {
        if (value == null) {
            return null;
        }
        return PredictionTheme.valueOf(value.trim().toUpperCase());
    }
}
