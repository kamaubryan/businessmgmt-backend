package BusinessSystem.Service;

import BusinessSystem.Enums.Status;
import BusinessSystem.Model.TaskHistory;
import BusinessSystem.Repository.TaskHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskHistoryService {

    private final TaskHistoryRepository taskHistoryRepository;

    public void record(Long taskId, Long actorId, String action, Status from, Status to) {
        TaskHistory h = new TaskHistory();
        h.setTaskId(taskId);
        h.setActorId(actorId);
        h.setAction(action);
        h.setFromStatus(from);
        h.setToStatus(to);
        taskHistoryRepository.save(h);
    }

    public List<TaskHistory> forTask(Long taskId) {
        return taskHistoryRepository.findByTaskIdOrderByAtAsc(taskId);
    }
}
