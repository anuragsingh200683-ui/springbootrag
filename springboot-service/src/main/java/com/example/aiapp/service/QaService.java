package com.example.aiapp.service;

import com.example.aiapp.dto.QaRequest;
import com.example.aiapp.dto.QaResponse;
import com.example.aiapp.dto.SourceChunkDto;
import com.example.aiapp.entity.QaHistory;
import com.example.aiapp.qa.QaAnswer;
import com.example.aiapp.qa.QaGraphService;
import com.example.aiapp.repository.QaHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class QaService {

    private static final Logger log = LoggerFactory.getLogger(QaService.class);

    private final QaGraphService qaGraphService;
    private final QaHistoryRepository qaHistoryRepository;
    private final QaCacheService qaCacheService;

    public QaService(QaGraphService qaGraphService, QaHistoryRepository qaHistoryRepository, QaCacheService qaCacheService) {
        this.qaGraphService = qaGraphService;
        this.qaHistoryRepository = qaHistoryRepository;
        this.qaCacheService = qaCacheService;
    }

    public QaResponse ask(QaRequest request) {
        Optional<QaResponse> cached = qaCacheService.get(request.getDocumentId(), request.getQuestion());
        if (cached.isPresent()) {
            log.info("Cache hit for question: {}", request.getQuestion());
            QaResponse response = cached.get();
            response.setCached(true);
            return response;
        }

        QaAnswer qaAnswer = qaGraphService.answer(request.getQuestion(), request.getDocumentId(), null);
        qaHistoryRepository.save(new QaHistory(request.getDocumentId(), request.getQuestion(), qaAnswer.answer()));

        List<SourceChunkDto> sources = qaAnswer.sources().stream()
                .map(s -> new SourceChunkDto(s.chunkIndex(), s.chunkText(), s.similarityScore()))
                .collect(Collectors.toList());

        QaResponse response = QaResponse.builder()
                .question(qaAnswer.question())
                .answer(qaAnswer.answer())
                .sources(sources)
                .model(qaAnswer.model())
                .cached(false)
                .build();

        qaCacheService.put(request.getDocumentId(), request.getQuestion(), response);
        return response;
    }
}
