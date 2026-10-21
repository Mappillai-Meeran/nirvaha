package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.dto.NotificationResponse;
import com.nirvaha.nirvaha_backend.dto.UnreadCountResponse;
import com.nirvaha.nirvaha_backend.entity.Notification;
import com.nirvaha.nirvaha_backend.entity.User;
import com.nirvaha.nirvaha_backend.event.IncidentLifecycleNotificationEvent;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.repository.NotificationRepository;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               org.springframework.transaction.support.TransactionTemplate transactionTemplate) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.transactionTemplate = transactionTemplate;
        this.transactionTemplate.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public Optional<NotificationResponse> createNotification(IncidentLifecycleNotificationEvent event) {
        if (event == null || event.getRecipientId() == null) {
            log.warn("Cannot create notification: event or recipient ID is null");
            return Optional.empty();
        }

        // Fast-path idempotency check via deduplication key
        if (event.getDeduplicationKey() != null && !event.getDeduplicationKey().isBlank()) {
            Optional<Notification> existing = notificationRepository.findByDeduplicationKey(event.getDeduplicationKey());
            if (existing.isPresent()) {
                log.debug("Duplicate notification skipped for key: {}", event.getDeduplicationKey());
                return Optional.of(NotificationResponse.fromEntity(existing.get()));
            }
        }

        try {
            return transactionTemplate.execute(status -> {
                Optional<User> recipientOpt = userRepository.findById(event.getRecipientId());
                if (recipientOpt.isEmpty()) {
                    log.warn("Recipient user with ID {} not found. Notification dropped.", event.getRecipientId());
                    return Optional.empty();
                }

                User recipient = recipientOpt.get();
                Notification notification = new Notification(
                        recipient,
                        event.getEventType(),
                        event.getTitle(),
                        event.getMessage(),
                        event.getIncidentId(),
                        event.getDeduplicationKey()
                );

                Notification saved = notificationRepository.saveAndFlush(notification);
                log.info("In-app notification created [ID={}, event={}, recipientId={}]", saved.getId(), saved.getEventType(), recipient.getId());
                return Optional.of(NotificationResponse.fromEntity(saved));
            });
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Check if failure was indeed a duplicate collision on deduplication_key
            if (event.getDeduplicationKey() != null && !event.getDeduplicationKey().isBlank()) {
                Optional<Notification> existing = notificationRepository.findByDeduplicationKey(event.getDeduplicationKey());
                if (existing.isPresent()) {
                    log.info("Duplicate notification detected via database unique constraint on deduplication_key: {}", event.getDeduplicationKey());
                    return Optional.of(NotificationResponse.fromEntity(existing.get()));
                }
            }
            log.error("Data integrity violation while persisting notification for incident {}: {}", event.getIncidentId(), e.getMessage(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsForCurrentUser(Pageable pageable) {
        User currentUser = getAuthenticatedUser();
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentUser.getId(), pageable)
                .map(NotificationResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCountForCurrentUser() {
        User currentUser = getAuthenticatedUser();
        long count = notificationRepository.countByRecipientIdAndReadFalse(currentUser.getId());
        return new UnreadCountResponse(count);
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        User currentUser = getAuthenticatedUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification with ID " + notificationId + " not found"));

        if (!notification.getRecipient().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access denied: You cannot modify notifications belonging to another user");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return NotificationResponse.fromEntity(notification);
    }

    @Transactional
    public int markAllAsReadForCurrentUser() {
        User currentUser = getAuthenticatedUser();
        return notificationRepository.markAllAsReadForRecipient(currentUser.getId(), LocalDateTime.now());
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Authentication required to access notifications");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found in municipal records"));
    }
}
