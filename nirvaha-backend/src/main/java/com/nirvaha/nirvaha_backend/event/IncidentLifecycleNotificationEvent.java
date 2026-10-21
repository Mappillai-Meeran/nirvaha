package com.nirvaha.nirvaha_backend.event;

import com.nirvaha.nirvaha_backend.entity.NotificationEventType;

public class IncidentLifecycleNotificationEvent {

    private final Long recipientId;
    private final NotificationEventType eventType;
    private final String title;
    private final String message;
    private final Long incidentId;
    private final String deduplicationKey;

    public IncidentLifecycleNotificationEvent(Long recipientId,
                                              NotificationEventType eventType,
                                              String title,
                                              String message,
                                              Long incidentId,
                                              String deduplicationKey) {
        this.recipientId = recipientId;
        this.eventType = eventType;
        this.title = title;
        this.message = message;
        this.incidentId = incidentId;
        this.deduplicationKey = deduplicationKey;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public NotificationEventType getEventType() {
        return eventType;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public String getDeduplicationKey() {
        return deduplicationKey;
    }
}
