package com.v_tourhub.notification_service.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.v_tourhub.notification_service.entity.NotificationLog;
import com.v_tourhub.notification_service.repository.NotificationLogRepository;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final NotificationLogRepository logRepo;

    public void sendBookingConfirmation(String to, Map<String, Object> variables) {
        String subject = "V-TourHub - Xác nhận đặt chỗ #" + variables.get("bookingId");
        
        // 1. Render HTML từ Template
        Context context = new Context();
        context.setVariables(variables);
        String htmlContent = templateEngine.process("booking-success", context);

        // 2. Gửi Email
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); 
            helper.setFrom("noreply@v-tourhub.com");

            mailSender.send(message);
            log.info("Email sent to {}", to);

            saveLog(to, subject, htmlContent, "SENT", null);

        } catch (Exception e) {
            log.error("Failed to send email", e);
            saveLog(to, subject, htmlContent, "FAILED", e.getMessage());
        }
    }

    private void saveLog(String to, String sub, String content, String status, String error) {
        NotificationLog log = NotificationLog.builder()
                .recipient(to)
                .subject(sub)
                .content(content) 
                .status(status)
                .errorMessage(error)
                .type("EMAIL")
                .build();
        logRepo.save(log);
    }
}