package com.example.aiapp.service;

import com.example.aiapp.dto.FastApiDeleteResponse;
import com.example.aiapp.dto.FastApiProcessRequest;
import com.example.aiapp.dto.FastApiProcessResponse;
import com.example.aiapp.dto.FastApiQueryRequest;
import com.example.aiapp.dto.FastApiQueryResponse;
import com.example.aiapp.exception.FastApiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Thin REST client wrapping calls to the Python FastAPI AI service.
 */
@Service
public class FastApiClientService {

    private static final Logger log = LoggerFactory.getLogger(FastApiClientService.class);

    private final WebClient webClient;

    public FastApiClientService(WebClient fastApiWebClient) {
        this.webClient = fastApiWebClient;
    }

    public FastApiProcessResponse processDocument(FastApiProcessRequest request) {
        log.info("Calling FastAPI /api/documents/process for document {}", request.getDocumentId());
        try {
            return webClient.post()
                    .uri("/api/documents/process")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(FastApiProcessResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("FastAPI returned error status {} for document processing: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new FastApiServiceException("Document processing failed: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Failed to reach FastAPI service for document processing", e);
            throw new FastApiServiceException("Could not reach the AI service. Is fastapi-service running on port 8000?", e);
        }
    }

    /**
     * Best-effort: unlike processDocument/askQuestion this does NOT throw on failure.
     * Postgres cascades document_chunks deletes when the `documents` row is removed
     * (see sql-scripts/init.sql), so if FastAPI is unreachable the document delete on
     * the Spring Boot side should still proceed rather than being blocked by this call.
     */
    public void deleteDocument(UUID documentId) {
        log.info("Calling FastAPI DELETE /api/documents/{} ", documentId);
        try {
            webClient.delete()
                    .uri("/api/documents/{documentId}", documentId)
                    .retrieve()
                    .bodyToMono(FastApiDeleteResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.warn("FastAPI returned error status {} while deleting document {}: {}",
                    e.getStatusCode(), documentId, e.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("Could not reach FastAPI service to delete document {}. Continuing with local delete; " +
                    "Postgres cascade will remove any remaining chunks.", documentId, e);
        }
    }

    public FastApiQueryResponse askQuestion(FastApiQueryRequest request) {
        log.info("Calling FastAPI /api/qa/query: {}", request.getQuestion());
        try {
            return webClient.post()
                    .uri("/api/qa/query")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(FastApiQueryResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("FastAPI returned error status {} for QA query: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new FastApiServiceException("Question answering failed: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Failed to reach FastAPI service for QA query", e);
            throw new FastApiServiceException("Could not reach the AI service. Is fastapi-service running on port 8000?", e);
        }
    }
}
