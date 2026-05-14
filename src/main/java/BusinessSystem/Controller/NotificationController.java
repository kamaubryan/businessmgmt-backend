package BusinessSystem.Controller;

import BusinessSystem.Model.Notification;
import BusinessSystem.Model.User;
import BusinessSystem.Security.UserPrincipal;
import BusinessSystem.Service.NotificationService;
import BusinessSystem.Web.CurrentUserHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<Notification>> list(@AuthenticationPrincipal UserPrincipal principal) {
        User user = CurrentUserHelper.require(principal);
        return ResponseEntity.ok(notificationService.forUser(user));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unread(@AuthenticationPrincipal UserPrincipal principal) {
        User user = CurrentUserHelper.require(principal);
        return ResponseEntity.ok(Map.of("count", notificationService.unreadCount(user)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markRead(@PathVariable Long id,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        User user = CurrentUserHelper.require(principal);
        return ResponseEntity.ok(notificationService.markRead(id, user));
    }
}
