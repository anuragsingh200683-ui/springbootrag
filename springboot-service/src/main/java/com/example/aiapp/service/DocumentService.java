package com.example.aiapp.service;

import com.example.aiapp.dto.FastApiProcessRequest;
import com.example.aiapp.dto.FastApiProcessResponse;
import com.example.aiapp.entity.Document;
import com.example.aiapp.entity.DocumentStatus;
import com.example.aiapp.exception.DocumentNotFoundException;
import com.example.aiapp.exception.FastApiServiceException;
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
    private final FastApiClientService fastApiClientService;

    public DocumentService(DocumentRepository documentRepository,
                            FileStorageService fileStorageService,
                            FastApiClientService fastApiClientService) {
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
        this.fastApiClientService = fastApiClientService;
    }

    /**
     * Deliberately NOT @Transactional: this method saves to Postgres, then makes an
     * external HTTP call to FastAPI, which uses its own separate database connection
     * to insert rows referencing the same document_id. If this whole method were wrapped
     * in one Spring-managed transaction, the initial insert wouldn't be committed (and
     * therefore not visible to FastAPI's connection) until after the HTTP call returns,
     * causing a foreign-key violation on the FastAPI side. Each repository.save() call
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
            FastApiProcessResponse response = fastApiClientService.processDocument(
                    new FastApiProcessRequest(documentId, filePath));

            document.setStatus(DocumentStatus.PROCESSED);
            document.setChunkCount(response.getChunkCount());
            document.setErrorMessage(null);
        } catch (FastApiServiceException e) {
            document.setStatus(DocumentStatus.FAILED);
            document.setErrorMessage(e.getMessage());
            log.error("Processing failed for document {}: {}", documentId, e.getMessage());
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
     * Mirror of uploadAndProcess: tells FastAPI to drop the indexed chunks (best-effort),
     * removes the stored PDF from disk, then deletes the metadata row. Deleting the row
     * cascades to any remaining document_chunks at the Postgres level (ON DELETE CASCADE
     * in sql-scripts/init.sql), so this stays correct even if the FastAPI call fails.
     */
    public void deleteDocument(UUID documentId) {
        Document document = getByDocumentId(documentId);

        fastApiClientService.deleteDocument(documentId);
        fileStorageService.deleteFile(document.getFilePath());
        documentRepository.delete(document);

        log.info("Deleted document {}", documentId);
    }
}
