package com.example.aiapp.dto;

import java.util.List;

/** Deserialized from FastAPI's POST /api/qa/query response. Field names here are single words
 *  (question, answer, model) so no @JsonProperty mapping is needed; "sources" uses a dedicated
 *  snake_case DTO since its nested fields (chunk_index, etc.) do need mapping. */
public class FastApiQueryResponse {
    private String question;
    private String answer;
    private List<FastApiSourceChunkDto> sources;
    private String model;

    public FastApiQueryResponse() {
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<FastApiSourceChunkDto> getSources() {
        return sources;
    }

    public void setSources(List<FastApiSourceChunkDto> sources) {
        this.sources = sources;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
