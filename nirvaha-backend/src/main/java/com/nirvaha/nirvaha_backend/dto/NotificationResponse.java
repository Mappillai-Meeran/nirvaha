package com.nirvaha.nirvaha_backend.dto;

import com.nirvaha.nirvaha_backend.entity.Notification;
import com.nirvaha.nirvaha_backend.entity.NotificationEventType;

import java.time.LocalDateTime;

public class NotificationResponse {

    private Long id;
    private Long recipientId;
    private NotificationEventType eventType;
    private String title;
    private String message;
    private Long incidentId;
    private boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public NotificationResponse() {
    }

    public static NotificationResponse fromEntity(Notification entity) {
        if (entity == null) {
            return null;
        }
        NotificationResponse resp = new NotificationResponse();
        resp.setId(entity.getId());
        resp.setRecipientId(entity.getRecipient() != null ? entity.getRecipient().getId() : null);
        resp.setEventType(entity.getEventType());
        resp.setTitle(entity.getTitle());
        resp.setMessage(entity.getMessage());
        resp.setIncidentId(entity.getIncidentId());
        resp.setRead(entity.isRead());
        resp.setReadAt(entity.getReadAt());
        resp.setCreatedAt(entity.getCreatedAt());
        return resp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public NotificationEventType getEventType() {
        return eventType;
    }

    public void setEventType(NotificationEventType eventType) {
        this.eventType = eventType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(Long incidentId) {
        this.incidentId = incidentId;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
