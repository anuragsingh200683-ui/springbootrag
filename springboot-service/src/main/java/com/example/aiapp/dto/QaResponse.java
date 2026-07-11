package com.example.aiapp.dto;

import java.util.List;

public class QaResponse {
    private String question;
    private String answer;
    private List<SourceChunkDto> sources;
    private String model;
    private boolean cached;

    public QaResponse() {
    }

    private QaResponse(Builder builder) {
        this.question = builder.question;
        this.answer = builder.answer;
        this.sources = builder.sources;
        this.model = builder.model;
        this.cached = builder.cached;
    }

    public static Builder builder() {
        return new Builder();
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

    public List<SourceChunkDto> getSources() {
        return sources;
    }

    public void setSources(List<SourceChunkDto> sources) {
        this.sources = sources;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isCached() {
        return cached;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }

    public static class Builder {
        private String question;
        private String answer;
        private List<SourceChunkDto> sources;
        private String model;
        private boolean cached;

        public Builder question(String question) {
            this.question = question;
            return this;
        }

        public Builder answer(String answer) {
            this.answer = answer;
            return this;
        }

        public Builder sources(List<SourceChunkDto> sources) {
            this.sources = sources;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder cached(boolean cached) {
            this.cached = cached;
            return this;
        }

        public QaResponse build() {
            return new QaResponse(this);
        }
    }
}
