package com.v_tourhub.notification_service.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.soa.common.event.BookingCancelledEvent;
import com.soa.common.event.BookingConfirmedEvent;
import com.soa.common.event.BookingFailedEvent;
import com.soa.common.event.BookingReadyForPaymentEvent;
import com.v_tourhub.notification_service.entity.NotificationLog;
import com.v_tourhub.notification_service.entity.NotificationStatus;
import com.v_tourhub.notification_service.entity.NotificationType;
import com.v_tourhub.notification_service.repository.NotificationLogRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final NotificationLogRepository logRepo;

    public void sendBookingConfirmation(BookingConfirmedEvent event) {
        String to = event.getCustomerEmail();
        String subject = "V-TourHub - Xác nhận đặt chỗ #" + event.getBookingId();

        Context context = new Context();
        context.setVariable("event", event);
        String htmlContent = templateEngine.process("booking-success", context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom("noreply@v-tourhub.com");

            mailSender.send(message);
            log.info("Email sent to {}", to);

            saveLog(to, subject, htmlContent, NotificationStatus.SENT, null, event.getBookingId());

        } catch (Exception e) {
            log.error("Failed to send email", e);
            saveLog(to, subject, htmlContent, NotificationStatus.FAILED, e.getMessage(), event.getBookingId());
        }
    }

    private void saveLog(String to, String sub, String content, NotificationStatus status, String error,
            Long bookingId) {
        NotificationLog notificationLog = NotificationLog.builder()
                .recipient(to)
                .subject(sub)
                .content(content)
                .status(status)
                .type(NotificationType.EMAIL)
                .errorMessage(error)
                .sentAt(LocalDateTime.now())
                .bookingId(bookingId)
                .build();
        logRepo.save(notificationLog);
    }

    public void sendBookingCancellationEmail(BookingCancelledEvent event) {
        String to = event.getCustomerEmail();
        String subject = "V-TourHub - Thông báo hủy đơn hàng #" + event.getBookingId();

        Context context = new Context();
        context.setVariable("event", event);
        String htmlContent = templateEngine.process("booking-cancelled", context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom("noreply@v-tourhub.com");

            mailSender.send(message);
            log.info("Email sent to {}", to);

            saveLog(to, subject, htmlContent, NotificationStatus.SENT, null, event.getBookingId());

        } catch (Exception e) {
            log.error("Failed to send email", e);
            saveLog(to, subject, htmlContent, NotificationStatus.FAILED, e.getMessage(), event.getBookingId());
        }
    }

    public void sendBookingFailureEmail(BookingFailedEvent event) {
        String to = event.getCustomerEmail();
        String subject = "V-TourHub - Thông báo: Đặt chỗ không thành công #" + event.getBookingId();

        Context context = new Context();
        context.setVariable("event", event);
        String htmlContent = templateEngine.process("booking-failed", context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom("noreply@v-tourhub.com");

            mailSender.send(message);
            log.info("Booking failure email sent to {}", to);
            saveLog(to, subject, htmlContent, NotificationStatus.SENT, null, event.getBookingId());
        } catch (Exception e) {
            log.error("Failed to send booking failure email", e);
            saveLog(to, subject, htmlContent, NotificationStatus.FAILED, e.getMessage(), event.getBookingId());
        }
    }

    public void sendPaymentReadyEmail(BookingReadyForPaymentEvent event) {
        String to = event.getCustomerEmail();
        if (to == null || to.isEmpty()) {
            return;
        }

        String subject = "V-TourHub - Yêu cầu thanh toán cho đơn hàng #" + event.getBookingId();

        Context context = new Context();
        context.setVariable("event", event);
        String htmlContent = templateEngine.process("payment-ready", context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom("noreply@v-tourhub.com");

            mailSender.send(message);
            log.info("Payment ready email sent to {}", to);
            saveLog(to, subject, htmlContent, NotificationStatus.SENT, null, event.getBookingId());
        } catch (Exception e) {
            log.error("Failed to send payment ready email", e);
            saveLog(to, subject, htmlContent, NotificationStatus.FAILED, e.getMessage(), event.getBookingId());
        }
    }
}