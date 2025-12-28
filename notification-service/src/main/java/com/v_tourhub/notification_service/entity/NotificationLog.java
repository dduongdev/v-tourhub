package com.v_tourhub.notification_service.entity;

import com.soa.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog extends BaseEntity {

    private Long bookingId; // For traceability
    private String recipient; // Email nhận
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status; // PENDING, SENT, FAILED, RETRY

    private String errorMessage;

    @Enumerated(EnumType.STRING)
    private NotificationType type; // EMAIL, SMS, PUSH, IN_APP

    private LocalDateTime sentAt;
    private Integer retryCount;
}