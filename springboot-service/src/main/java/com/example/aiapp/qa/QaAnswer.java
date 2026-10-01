package com.example.aiapp.qa;

import com.example.aiapp.ingest.SemanticSearchResult;

import java.util.List;

public record QaAnswer(String question, String answer, List<SemanticSearchResult> sources, String model) {
}
