package com.nirvaha.nirvaha_backend.controller;

import com.nirvaha.nirvaha_backend.dto.NotificationResponse;
import com.nirvaha.nirvaha_backend.dto.UnreadCountResponse;
import com.nirvaha.nirvaha_backend.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        int boundedSize = Math.min(Math.max(1, size), 100);
        int boundedPage = Math.max(0, page);
        Pageable pageable = PageRequest.of(boundedPage, boundedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(notificationService.getNotificationsForCurrentUser(pageable));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount() {
        return ResponseEntity.ok(notificationService.getUnreadCountForCurrentUser());
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsReadPatch(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsReadPut(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsReadPatch() {
        int count = notificationService.markAllAsReadForCurrentUser();
        return ResponseEntity.ok(Map.of(
                "message", "All notifications marked as read",
                "markedCount", count
        ));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsReadPut() {
        int count = notificationService.markAllAsReadForCurrentUser();
        return ResponseEntity.ok(Map.of(
                "message", "All notifications marked as read",
                "markedCount", count
        ));
    }
}
