package com.v_tourhub.notification_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.v_tourhub.notification_service.entity.NotificationLog;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    
}
