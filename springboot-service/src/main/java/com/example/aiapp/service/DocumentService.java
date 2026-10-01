package com.example.aiapp.service;

import com.example.aiapp.entity.Document;
import com.example.aiapp.entity.DocumentStatus;
import com.example.aiapp.exception.DocumentNotFoundException;
import com.example.aiapp.exception.EmbeddingGenerationException;
import com.example.aiapp.exception.PdfExtractionException;
import com.example.aiapp.ingest.DocumentIngestionService;
import com.example.aiapp.ingest.VectorStoreRepository;
import com.example.aiapp.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final DocumentIngestionService documentIngestionService;
    private final VectorStoreRepository vectorStoreRepository;

    public DocumentService(DocumentRepository documentRepository,
                            FileStorageService fileStorageService,
                            DocumentIngestionService documentIngestionService,
                            VectorStoreRepository vectorStoreRepository) {
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
        this.documentIngestionService = documentIngestionService;
        this.vectorStoreRepository = vectorStoreRepository;
    }

    /**
     * Deliberately NOT @Transactional: PDF extraction/embedding/chunk insertion below
     * runs its own separate JDBC statements (via VectorStoreRepository), and a failure
     * partway through should still leave the document row updated with FAILED status
     * rather than being rolled back together with it. Each repository.save() call
     * below is transactional on its own (via Spring Data JPA) and commits immediately.
     */
    public Document uploadAndProcess(MultipartFile file) {
        UUID documentId = UUID.randomUUID();
        String filePath = fileStorageService.storePdf(file, documentId);

        Document document = Document.builder()
                .documentId(documentId)
                .fileName(file.getOriginalFilename())
                .filePath(filePath)
                .contentType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .status(DocumentStatus.UPLOADED)
                .build();
        document = documentRepository.save(document);
        log.info("Saved document metadata: {}", document.getDocumentId());

        document.setStatus(DocumentStatus.PROCESSING);
        document = documentRepository.save(document);

        try {
            int chunkCount = documentIngestionService.process(documentId, filePath);

            document.setStatus(DocumentStatus.PROCESSED);
            document.setChunkCount(chunkCount);
            document.setErrorMessage(null);
        } catch (PdfExtractionException | EmbeddingGenerationException e) {
            // Only the specific, expected failure modes DocumentIngestionService.process()
            // is documented to throw are caught here and recorded as an ordinary
            // per-document failure (bad PDF, embedding API hiccup). A bare
            // catch(RuntimeException) here previously also swallowed unrelated
            // programming bugs and genuine setup/config problems (e.g. a
            // NullPointerException, or a DataAccessException from a pgvector
            // dimension mismatch) and mislabeled them the same way - see the
            // catch(RuntimeException) below for how those are now handled instead.
            document.setStatus(DocumentStatus.FAILED);
            document.setErrorMessage(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            log.error("Processing failed for document {}", documentId, e);
        } catch (RuntimeException e) {
            // Anything NOT in the expected-failure list above is a bug or a setup
            // problem, not a normal ingestion failure - mark the document FAILED so
            // it does not sit stuck in PROCESSING forever, but rethrow so it still
            // surfaces loudly as a 500 via GlobalExceptionHandler's generic handler
            // instead of being silently mislabeled as an ordinary per-document error.
            document.setStatus(DocumentStatus.FAILED);
            document.setErrorMessage("Unexpected error during processing - see server logs.");
            documentRepository.save(document);
            log.error("Unexpected error processing document {}", documentId, e);
            throw e;
        }

        return documentRepository.save(document);
    }

    public Document getByDocumentId(UUID documentId) {
        return documentRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
    }

    public List<Document> listAll() {
        return documentRepository.findAll();
    }

    /**
     * Best-effort chunk cleanup, then removes the stored PDF from disk and the metadata
     * row. Deleting the row also cascades to any remaining document_chunks at the
     * Postgres level (ON DELETE CASCADE in sql-scripts/init.sql), so this stays correct
     * even if the explicit chunk delete below fails.
     */
    public void deleteDocument(UUID documentId) {
        Document document = getByDocumentId(documentId);

        try {
            vectorStoreRepository.deleteByDocumentId(documentId);
        } catch (RuntimeException e) {
            log.warn("Failed to delete chunks for document {}; relying on cascade delete: {}",
                    documentId, e.getMessage());
        }
        fileStorageService.deleteFile(document.getFilePath());
        documentRepository.delete(document);

        log.info("Deleted document {}", documentId);
    }
}
