package com.v_tourhub.notification_service.entity;

import com.soa.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "notification_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog extends BaseEntity {

    private String recipient; // Email nhận
    private String subject;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    private String status; // SENT, FAILED
    private String errorMessage;
    private String type; // EMAIL, SMS
}