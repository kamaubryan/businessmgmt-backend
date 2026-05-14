package BusinessSystem.Service;

import BusinessSystem.Model.Notification;
import BusinessSystem.Model.User;
import BusinessSystem.Repository.NotificationRepository;
import BusinessSystem.Web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Notification notify(User user, Long taskId, String message) {
        Notification n = new Notification();
        n.setUser(user);
        n.setTaskId(taskId);
        n.setMessage(message);
        n.setRead(false);
        return notificationRepository.save(n);
    }

    public List<Notification> forUser(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public long unreadCount(User user) {
        return notificationRepository.countByUserAndReadFalse(user);
    }

    public Notification markRead(Long id, User user) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!n.getUser().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your notification");
        }
        n.setRead(true);
        return notificationRepository.save(n);
    }
}
