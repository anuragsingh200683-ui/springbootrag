package com.example.aiapp.ai;

import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import com.openai.models.embeddings.Embedding;
import com.openai.models.embeddings.EmbeddingCreateParams;
import com.openai.models.embeddings.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Wraps OpenAI's embeddings endpoint (text-embedding-3-small, 1536 dimensions),
 * replacing fastapi-service's local sentence-transformers model.
 */
@Component
public class OpenAiEmbeddingClient {

    private final OpenAIClient client;
    private final EmbeddingModel model;

    public OpenAiEmbeddingClient(OpenAIClient client, @Value("${app.openai.embedding-model}") String model) {
        this.client = client;
        this.model = EmbeddingModel.of(model);
    }

    public float[] embed(String text) {
        return embedBatch(List.of(text)).get(0);
    }

    public List<float[]> embedBatch(List<String> texts) {
        EmbeddingCreateParams params = EmbeddingCreateParams.builder()
                .inputOfArrayOfStrings(texts)
                .model(model)
                .build();
        CreateEmbeddingResponse response = client.embeddings().create(params);
        return response.data().stream()
                .map(this::toFloatArray)
                .toList();
    }

    private float[] toFloatArray(Embedding embedding) {
        List<Float> values = embedding.embedding();
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }
}
