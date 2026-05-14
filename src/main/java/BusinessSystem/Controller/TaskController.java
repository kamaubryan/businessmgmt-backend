package BusinessSystem.Controller;

import BusinessSystem.Model.Task;
import BusinessSystem.Model.TaskHistory;
import BusinessSystem.Model.User;
import BusinessSystem.Security.UserPrincipal;
import BusinessSystem.Service.TaskHistoryService;
import BusinessSystem.Service.TaskService;
import BusinessSystem.Web.CurrentUserHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskHistoryService taskHistoryService;

    @PostMapping
    public ResponseEntity<Task> create(@RequestBody TaskService.TaskRequest req,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        User user = CurrentUserHelper.require(principal);
        return ResponseEntity.ok(taskService.createTask(req, user));
    }

    @GetMapping
    public ResponseEntity<List<Task>> list(@AuthenticationPrincipal UserPrincipal principal) {
        User user = CurrentUserHelper.require(principal);
        return ResponseEntity.ok(taskService.visibleTo(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> get(@PathVariable Long id,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        User user = CurrentUserHelper.require(principal);
        return ResponseEntity.ok(taskService.getVisible(id, user));
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<Task> assign(@PathVariable Long id,
                                       @RequestParam Long employeeId,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        User user = CurrentUserHelper.require(principal);
        return ResponseEntity.ok(taskService.assign(id, employeeId, user));
    }

    @PutMapping("/{id}/start")
    public ResponseEntity<Task> start(@PathVariable Long id,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        User user = CurrentUserHelper.require(principal);
        return ResponseEntity.ok(taskService.start(id, user));
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<Task> resolve(@PathVariable Long id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        User user = CurrentUserHelper.require(principal);
        return ResponseEntity.ok(taskService.resolve(id, user));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<Task> confirm(@PathVariable Long id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        User user = CurrentUserHelper.require(principal);
        return ResponseEntity.ok(taskService.confirm(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        User user = CurrentUserHelper.require(principal);
        taskService.delete(id, user);
        return ResponseEntity.ok(Map.of("message", "Task deleted"));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<TaskHistory>> history(@PathVariable Long id,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        User user = CurrentUserHelper.require(principal);
        taskService.getVisible(id, user);
        return ResponseEntity.ok(taskHistoryService.forTask(id));
    }
}
