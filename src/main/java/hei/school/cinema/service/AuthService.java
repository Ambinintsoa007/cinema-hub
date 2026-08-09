package hei.school.cinema.service;

import hei.school.cinema.endpoint.rest.dto.LoginRequest;
import hei.school.cinema.endpoint.rest.dto.RegisterRequest;
import hei.school.cinema.exception.ApiException;
import hei.school.cinema.mapper.UserMapper;
import hei.school.cinema.model.User;
import hei.school.cinema.model.UserRole;
import hei.school.cinema.model.UserStatus;
import hei.school.cinema.repository.UserRepository;
import hei.school.cinema.repository.model.UserEntity;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(RegisterRequest request) {
        validateRegisterRequest(request);

        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Email is already used");
        }

        User user =
                User.builder()
                        .id(UUID.randomUUID())
                        .firstName(request.getFirstName().trim())
                        .lastName(request.getLastName().trim())
                        .birthdate(request.getBirthdate())
                        .email(email)
                        .phone(request.getPhone().trim())
                        .passwordHash(passwordEncoder.encode(request.getPassword()))
                        .role(UserRole.CLIENT)
                        .status(UserStatus.ACTIVE)
                        .build();

        return userMapper.toDomain(userRepository.save(userMapper.toEntity(user)));
    }

    @Transactional(readOnly = true)
    public User authenticate(LoginRequest request) {
        validateLoginRequest(request);

        String email = normalizeEmail(request.getEmail());

        UserEntity userEntity =
                userRepository
                        .findByEmailIgnoreCase(email)
                        .orElseThrow(() -> ApiException.unauthorized("Invalid credentials"));

        User user = userMapper.toDomain(userEntity);

        if (user.getStatus() == UserStatus.DISABLED) {
            throw ApiException.unauthorized("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid credentials");
        }

        return user;
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Registration data must not be null");
        }

        requireNotBlank(request.getFirstName(), "First name must not be blank");
        requireNotBlank(request.getLastName(), "Last name must not be blank");
        requireNotBlank(request.getEmail(), "Email must not be blank");
        requireNotBlank(request.getPhone(), "Phone must not be blank");
        requireNotBlank(request.getPassword(), "Password must not be blank");

        if (request.getBirthdate() == null) {
            throw ApiException.badRequest("Birthdate must not be null");
        }

        if (!request.getBirthdate().isBefore(LocalDate.now())) {
            throw ApiException.badRequest("Birthdate must be in the past");
        }

        if (!EMAIL_PATTERN.matcher(request.getEmail().trim()).matches()) {
            throw ApiException.badRequest("Invalid email format");
        }

        if (request.getPassword().length() < 8) {
            throw ApiException.badRequest("Password must contain at least 8 characters");
        }
    }

    private void validateLoginRequest(LoginRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Login data must not be null");
        }

        requireNotBlank(request.getEmail(), "Email must not be blank");
        requireNotBlank(request.getPassword(), "Password must not be blank");

        if (!EMAIL_PATTERN.matcher(request.getEmail().trim()).matches()) {
            throw ApiException.badRequest("Invalid email format");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void requireNotBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw ApiException.badRequest(message);
        }
    }
}