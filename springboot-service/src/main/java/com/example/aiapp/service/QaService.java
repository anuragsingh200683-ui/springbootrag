package com.example.aiapp.service;

import com.example.aiapp.dto.FastApiQueryRequest;
import com.example.aiapp.dto.FastApiQueryResponse;
import com.example.aiapp.dto.FastApiSourceChunkDto;
import com.example.aiapp.dto.QaRequest;
import com.example.aiapp.dto.QaResponse;
import com.example.aiapp.dto.SourceChunkDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class QaService {

    private static final Logger log = LoggerFactory.getLogger(QaService.class);

    private final FastApiClientService fastApiClientService;
    private final QaCacheService qaCacheService;

    public QaService(FastApiClientService fastApiClientService, QaCacheService qaCacheService) {
        this.fastApiClientService = fastApiClientService;
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

        FastApiQueryResponse fastApiResponse = fastApiClientService.askQuestion(
                new FastApiQueryRequest(request.getDocumentId(), request.getQuestion(), null));

        List<SourceChunkDto> sources = fastApiResponse.getSources() == null
                ? List.of()
                : fastApiResponse.getSources().stream()
                    .map(s -> new SourceChunkDto(s.getChunkIndex(), s.getChunkText(), s.getSimilarityScore()))
                    .collect(Collectors.toList());

        QaResponse response = QaResponse.builder()
                .question(fastApiResponse.getQuestion())
                .answer(fastApiResponse.getAnswer())
                .sources(sources)
                .model(fastApiResponse.getModel())
                .cached(false)
                .build();

        qaCacheService.put(request.getDocumentId(), request.getQuestion(), response);
        return response;
    }
}
