package com.example.aiapp.qa;

/**
 * The five routes the classifier can pick between, ported from
 * fastapi-service/qa_graph/state.py's RouteCategory literal.
 */
public enum RouteCategory {
    RAG,
    DATABASE,
    REST_API,
    TOOL,
    GENERAL;

    public static RouteCategory fromWireValue(String value) {
        if (value == null) {
            return GENERAL;
        }
        return switch (value.trim().toLowerCase()) {
            case "rag" -> RAG;
            case "database" -> DATABASE;
            case "rest_api" -> REST_API;
            case "tool" -> TOOL;
            default -> GENERAL;
        };
    }
}
