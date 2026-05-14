package BusinessSystem.Model;

import BusinessSystem.Enums.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "task_history")
public class TaskHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long taskId;

    private Long actorId;

    @Column(nullable = false)
    private String action;

    @Enumerated(EnumType.STRING)
    private Status fromStatus;

    @Enumerated(EnumType.STRING)
    private Status toStatus;

    @Column(nullable = false)
    private Timestamp at;

    @PrePersist
    protected void onCreate() {
        if (at == null) {
            at = new Timestamp(System.currentTimeMillis());
        }
    }
}
