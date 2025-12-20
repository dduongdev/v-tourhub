package com.v_tourhub.catalog_service.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.v_tourhub.catalog_service.entity.OutboxEvent;
import com.v_tourhub.catalog_service.repository.OutboxEventRepository;

@Service
@RequiredArgsConstructor
public class EventPublisherService {

    private final OutboxEventRepository outboxRepo;
    private final ObjectMapper objectMapper; 

    @SneakyThrows
    public void saveEventToOutbox(String aggregateType, String aggregateId, String eventType, Object payload) {
        String payloadJson = objectMapper.writeValueAsString(payload);
        
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payloadJson)
                .build();
                
        outboxRepo.save(outboxEvent);
    }
}