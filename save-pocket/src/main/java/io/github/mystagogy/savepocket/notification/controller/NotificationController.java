package io.github.mystagogy.savepocket.notification.controller;

import io.github.mystagogy.savepocket.common.response.ApiResponse;
import io.github.mystagogy.savepocket.common.security.CurrentUserProvider;
import io.github.mystagogy.savepocket.notification.dto.NotificationListResponse;
import io.github.mystagogy.savepocket.notification.dto.NotificationReadResponse;
import io.github.mystagogy.savepocket.notification.service.NotificationService;
import io.github.mystagogy.savepocket.notification.service.NotificationSseService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationSseService notificationSseService;
    private final CurrentUserProvider currentUserProvider;

    public NotificationController(
            NotificationService notificationService,
            NotificationSseService notificationSseService,
            CurrentUserProvider currentUserProvider
    ) {
        this.notificationService = notificationService;
        this.notificationSseService = notificationSseService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationListResponse>> getNotifications(
            @RequestParam(required = false) Integer limit
    ) {
        Long userId = currentUserProvider.getCurrentUserId();
        NotificationListResponse response = notificationService.getNotifications(userId, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationReadResponse>> markNotificationAsRead(@PathVariable Long id) {
        Long userId = currentUserProvider.getCurrentUserId();
        NotificationReadResponse response = notificationService.markAsRead(userId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications() {
        Long userId = currentUserProvider.getCurrentUserId();
        return notificationSseService.subscribe(userId);
    }
}
