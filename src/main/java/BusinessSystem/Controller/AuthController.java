package BusinessSystem.Controller;

import BusinessSystem.Model.User;
import BusinessSystem.Security.UserPrincipal;
import BusinessSystem.Web.CurrentUserHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@AuthenticationPrincipal UserPrincipal principal) {
        User user = CurrentUserHelper.require(principal);
        return ResponseEntity.ok(Map.of(
                "message", "Login successful",
                "user", user));
    }

    @GetMapping("/me")
    public ResponseEntity<User> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(CurrentUserHelper.require(principal));
    }
}
