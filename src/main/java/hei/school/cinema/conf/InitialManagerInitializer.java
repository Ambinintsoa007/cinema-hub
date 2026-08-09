package hei.school.cinema.conf;

import hei.school.cinema.mapper.UserMapper;
import hei.school.cinema.model.User;
import hei.school.cinema.model.UserRole;
import hei.school.cinema.model.UserStatus;
import hei.school.cinema.repository.UserRepository;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InitialManagerInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.initial-manager.enabled:true}")
    private boolean enabled;

    @Value("${app.initial-manager.first-name:}")
    private String firstName;

    @Value("${app.initial-manager.last-name:}")
    private String lastName;

    @Value("${app.initial-manager.birthdate:}")
    private String birthdate;

    @Value("${app.initial-manager.email:}")
    private String email;

    @Value("${app.initial-manager.phone:}")
    private String phone;

    @Value("${app.initial-manager.password:}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled || !isFullyConfigured()) {
            return;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return;
        }

        LocalDate parsedBirthdate = parseBirthdate();

        if (!parsedBirthdate.isBefore(LocalDate.now())) {
            throw new IllegalStateException("Initial manager birthdate must be in the past");
        }

        if (password.length() < 8) {
            throw new IllegalStateException(
                    "Initial manager password must contain at least 8 characters");
        }

        User manager =
                User.builder()
                        .id(UUID.randomUUID())
                        .firstName(firstName.trim())
                        .lastName(lastName.trim())
                        .birthdate(parsedBirthdate)
                        .email(normalizedEmail)
                        .phone(phone.trim())
                        .passwordHash(passwordEncoder.encode(password))
                        .role(UserRole.MANAGER)
                        .status(UserStatus.ACTIVE)
                        .build();

        userRepository.save(userMapper.toEntity(manager));
    }

    private boolean isFullyConfigured() {
        return !isBlank(firstName)
                && !isBlank(lastName)
                && !isBlank(birthdate)
                && !isBlank(email)
                && !isBlank(phone)
                && !isBlank(password);
    }

    private LocalDate parseBirthdate() {
        try {
            return LocalDate.parse(birthdate.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalStateException(
                    "Initial manager birthdate must use yyyy-MM-dd format", exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}