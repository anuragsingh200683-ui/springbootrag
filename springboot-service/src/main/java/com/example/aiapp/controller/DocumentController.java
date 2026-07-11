package com.example.aiapp.controller;

import com.example.aiapp.dto.DocumentResponse;
import com.example.aiapp.entity.Document;
import com.example.aiapp.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<DocumentResponse> uploadDocument(@RequestParam("file") MultipartFile file) {
        log.info("Received upload request: {} ({} bytes)", file.getOriginalFilename(), file.getSize());
        Document document = documentService.uploadAndProcess(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentResponse.fromEntity(document));
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable UUID documentId) {
        Document document = documentService.getByDocumentId(documentId);
        return ResponseEntity.ok(DocumentResponse.fromEntity(document));
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> listDocuments() {
        List<DocumentResponse> documents = documentService.listAll().stream()
                .map(DocumentResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(documents);
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID documentId) {
        log.info("Received delete request for document {}", documentId);
        documentService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }
}
