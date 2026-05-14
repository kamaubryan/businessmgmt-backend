package BusinessSystem.Config;

import BusinessSystem.Enums.Role;
import BusinessSystem.Model.User;
import BusinessSystem.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.supervisor-email}")
    private String supervisorEmail;

    @Value("${app.bootstrap.supervisor-password}")
    private String supervisorPassword;

    @Override
    public void run(String... args) {
        if (userRepository.countByRole(Role.SUPERVISOR) > 0) {
            return;
        }
        User u = new User();
        u.setFirstName("Initial");
        u.setLastName("Supervisor");
        u.setEmail(supervisorEmail);
        u.setPassword(passwordEncoder.encode(supervisorPassword));
        u.setRole(Role.SUPERVISOR);
        userRepository.save(u);
        log.info("Seeded initial supervisor: {}", supervisorEmail);
    }
}
