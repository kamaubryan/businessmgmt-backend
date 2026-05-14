package BusinessSystem.Controller;

import BusinessSystem.Enums.Role;
import BusinessSystem.Model.User;
import BusinessSystem.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PreAuthorize("hasRole('SUPERVISOR')")
    @PostMapping
    public ResponseEntity<User> createEmployee(@RequestBody User user) {
        if (user.getRole() == null) {
            user.setRole(Role.EMPLOYEE);
        }
        return ResponseEntity.ok(userService.createUser(user));
    }

    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(@RequestParam(required = false) Role role) {
        return ResponseEntity.ok(role == null ? userService.getAllUsers() : userService.getUsersByRole(role));
    }

    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PreAuthorize("hasRole('SUPERVISOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "User deleted"));
    }

    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> counts() {
        return ResponseEntity.ok(Map.of(
                "employees", userService.countByRole(Role.EMPLOYEE),
                "supervisors", userService.countByRole(Role.SUPERVISOR)));
    }
}
