package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.event.IncidentLifecycleNotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIncidentLifecycleEvent(IncidentLifecycleNotificationEvent event) {
        log.debug("Received incident lifecycle event after transaction commit: {} for recipient {}",
                event.getEventType(), event.getRecipientId());
        try {
            notificationService.createNotification(event);
        } catch (Exception e) {
            log.error("Failed to persist notification for event {}: {}", event.getEventType(), e.getMessage(), e);
        }
    }
}
