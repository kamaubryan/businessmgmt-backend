package BusinessSystem.Service;

import BusinessSystem.Enums.Priority;
import BusinessSystem.Enums.Role;
import BusinessSystem.Enums.Status;
import BusinessSystem.Model.Task;
import BusinessSystem.Model.User;
import BusinessSystem.Repository.TaskRepository;
import BusinessSystem.Repository.UserRepository;
import BusinessSystem.Web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final TaskHistoryService taskHistoryService;

    public record TaskRequest(String title, String description, Priority priority,
                              LocalDate dueDate, Long assignedTo) {
    }

    @Transactional
    public Task createTask(TaskRequest req, User creator) {
        if (req.title() == null || req.title().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Title is required");
        }

        Task task = new Task();
        task.setTitle(req.title());
        task.setDescription(req.description());
        task.setPriority(req.priority() != null ? req.priority() : Priority.MEDIUM);
        task.setDueDate(req.dueDate());
        task.setStatus(Status.CREATED);
        task.setCreatedBy(creator);
        task.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        task.setUpdatedAt(task.getCreatedAt());

        if (req.assignedTo() != null) {
            User employee = loadEmployee(req.assignedTo());
            task.setAssignedTo(employee);
            task.setStatus(Status.ASSIGNED);
        }

        Task saved = taskRepository.save(task);
        taskHistoryService.record(saved.getId(), creator.getId(), "CREATED", null, Status.CREATED);

        if (saved.getAssignedTo() != null) {
            taskHistoryService.record(saved.getId(), creator.getId(), "ASSIGNED", Status.CREATED, Status.ASSIGNED);
            notificationService.notify(saved.getAssignedTo(), saved.getId(),
                    "You have been assigned a new task: " + saved.getTitle());
        }
        return saved;
    }

    @Transactional
    public Task assign(Long taskId, Long employeeId, User supervisor) {
        Task task = require(taskId);
        requireSupervisor(supervisor);
        if (task.getStatus() != Status.CREATED && task.getStatus() != Status.ASSIGNED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Cannot reassign a task that is " + task.getStatus());
        }

        User employee = loadEmployee(employeeId);
        Status from = task.getStatus();
        task.setAssignedTo(employee);
        task.setStatus(Status.ASSIGNED);
        task.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        Task saved = taskRepository.save(task);
        taskHistoryService.record(saved.getId(), supervisor.getId(), "ASSIGNED", from, Status.ASSIGNED);
        notificationService.notify(employee, saved.getId(),
                "You have been assigned a task: " + saved.getTitle());
        return saved;
    }

    @Transactional
    public Task start(Long taskId, User employee) {
        Task task = require(taskId);
        requireAssignee(task, employee);
        if (task.getStatus() != Status.ASSIGNED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Task must be ASSIGNED to start, was " + task.getStatus());
        }
        return transition(task, employee, Status.IN_PROGRESS, "STARTED");
    }

    @Transactional
    public Task resolve(Long taskId, User employee) {
        Task task = require(taskId);
        requireAssignee(task, employee);
        if (task.getStatus() != Status.IN_PROGRESS) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Task must be IN_PROGRESS to resolve, was " + task.getStatus());
        }
        Task saved = transition(task, employee, Status.RESOLVED, "RESOLVED");
        if (saved.getCreatedBy() != null) {
            notificationService.notify(saved.getCreatedBy(), saved.getId(),
                    "Task submitted for review: " + saved.getTitle());
        }
        return saved;
    }

    @Transactional
    public Task confirm(Long taskId, User supervisor) {
        Task task = require(taskId);
        requireSupervisor(supervisor);
        if (!task.getCreatedBy().getId().equals(supervisor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the creating supervisor can confirm");
        }
        if (task.getStatus() != Status.RESOLVED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Task must be RESOLVED before confirming, was " + task.getStatus());
        }
        Task saved = transition(task, supervisor, Status.DONE, "CONFIRMED");
        if (saved.getAssignedTo() != null) {
            notificationService.notify(saved.getAssignedTo(), saved.getId(),
                    "Task confirmed complete: " + saved.getTitle());
        }
        return saved;
    }

    public List<Task> visibleTo(User user) {
        if (user.getRole() == Role.SUPERVISOR) {
            return taskRepository.findAll();
        }
        return taskRepository.findByAssignedTo(user);
    }

    public Task getVisible(Long id, User user) {
        Task task = require(id);
        if (user.getRole() == Role.SUPERVISOR) return task;
        if (task.getAssignedTo() != null && task.getAssignedTo().getId().equals(user.getId())) return task;
        throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to view this task");
    }

    @Transactional
    public void delete(Long taskId, User supervisor) {
        Task task = require(taskId);
        requireSupervisor(supervisor);
        if (!task.getCreatedBy().getId().equals(supervisor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the creating supervisor can delete");
        }
        taskRepository.delete(task);
    }

    private Task transition(Task task, User actor, Status to, String action) {
        Status from = task.getStatus();
        task.setStatus(to);
        task.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        Task saved = taskRepository.save(task);
        taskHistoryService.record(saved.getId(), actor.getId(), action, from, to);
        return saved;
    }

    private Task require(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    private User loadEmployee(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Employee not found"));
        if (u.getRole() != Role.EMPLOYEE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "User is not an EMPLOYEE");
        }
        return u;
    }

    private void requireSupervisor(User user) {
        if (user.getRole() != Role.SUPERVISOR) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Supervisor role required");
        }
    }

    private void requireAssignee(Task task, User user) {
        if (task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the assigned employee can do this");
        }
    }
}