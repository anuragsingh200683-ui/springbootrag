package com.example.aiapp.controller;

import com.example.aiapp.dto.QaRequest;
import com.example.aiapp.dto.QaResponse;
import com.example.aiapp.service.QaService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/qa")
public class QaController {

    private static final Logger log = LoggerFactory.getLogger(QaController.class);

    private final QaService qaService;

    public QaController(QaService qaService) {
        this.qaService = qaService;
    }

    @PostMapping("/ask")
    public ResponseEntity<QaResponse> ask(@Valid @RequestBody QaRequest request) {
        log.info("QA request: documentId={}, question={}", request.getDocumentId(), request.getQuestion());
        QaResponse response = qaService.ask(request);
        return ResponseEntity.ok(response);
    }
}
