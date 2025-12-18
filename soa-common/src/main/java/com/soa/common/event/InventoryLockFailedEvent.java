package com.soa.common.event;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryLockFailedEvent {
    private Long bookingId;
    private String reason; 
}