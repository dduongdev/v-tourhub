package com.soa.payment_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soa.payment_service.entity.OutboxEvent;
import com.soa.payment_service.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

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