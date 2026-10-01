package com.example.aiapp.ingest;

import com.example.aiapp.ai.OpenAiEmbeddingClient;
import com.example.aiapp.exception.EmbeddingGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * In-process replacement for fastapi-service's POST /api/documents/process: extract
 * text, chunk it, embed each chunk, and store the chunks. Called by
 * DocumentService.uploadAndProcess() instead of proxying to FastAPI.
 */
@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final PdfTextExtractor pdfTextExtractor;
    private final TextChunker textChunker;
    private final OpenAiEmbeddingClient embeddingClient;
    private final VectorStoreRepository vectorStoreRepository;

    public DocumentIngestionService(PdfTextExtractor pdfTextExtractor,
                                     TextChunker textChunker,
                                     OpenAiEmbeddingClient embeddingClient,
                                     VectorStoreRepository vectorStoreRepository) {
        this.pdfTextExtractor = pdfTextExtractor;
        this.textChunker = textChunker;
        this.embeddingClient = embeddingClient;
        this.vectorStoreRepository = vectorStoreRepository;
    }

    public int process(UUID documentId, String filePath) {
        String text = pdfTextExtractor.extractText(filePath);
        List<String> chunkTexts = textChunker.split(text);
        if (chunkTexts.isEmpty()) {
            log.warn("No extractable text found in document {}", documentId);
            return 0;
        }

        List<float[]> embeddings;
        try {
            embeddings = embeddingClient.embedBatch(chunkTexts);
        } catch (Exception e) {
            throw new EmbeddingGenerationException(
                    "Failed to generate embeddings for document " + documentId + ": " + e.getMessage(), e);
        }

        List<ChunkRecord> chunks = new ArrayList<>(chunkTexts.size());
        for (int i = 0; i < chunkTexts.size(); i++) {
            chunks.add(new ChunkRecord(i, chunkTexts.get(i), embeddings.get(i)));
        }
        vectorStoreRepository.insertChunks(documentId, chunks);

        log.info("Processed document {} into {} chunks", documentId, chunks.size());
        return chunks.size();
    }
}
