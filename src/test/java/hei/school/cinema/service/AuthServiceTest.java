package hei.school.cinema.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import hei.school.cinema.endpoint.rest.dto.LoginRequest;
import hei.school.cinema.endpoint.rest.dto.RegisterRequest;
import hei.school.cinema.exception.ApiException;
import hei.school.cinema.mapper.UserMapper;
import hei.school.cinema.model.User;
import hei.school.cinema.model.UserRole;
import hei.school.cinema.model.UserStatus;
import hei.school.cinema.repository.UserRepository;
import hei.school.cinema.repository.model.UserEntity;
import hei.school.cinema.repository.model.UserRoleEntity;
import hei.school.cinema.repository.model.UserStatusEntity;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;

    private AuthService authService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();

        authService =
                new AuthService(
                        userRepository,
                        new UserMapper(),
                        passwordEncoder);
    }

    @Test
    void register_should_create_active_client_with_hashed_password() {
        RegisterRequest request =
                new RegisterRequest(
                        " John ",
                        " Doe ",
                        LocalDate.of(2000, 1, 1),
                        " JOHN@EXAMPLE.COM ",
                        " +261340000000 ",
                        "password123");

        when(userRepository.existsByEmailIgnoreCase("john@example.com"))
                .thenReturn(false);

        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User created = authService.register(request);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getFirstName()).isEqualTo("John");
        assertThat(created.getLastName()).isEqualTo("Doe");
        assertThat(created.getEmail()).isEqualTo("john@example.com");
        assertThat(created.getPhone()).isEqualTo("+261340000000");
        assertThat(created.getRole()).isEqualTo(UserRole.CLIENT);
        assertThat(created.getStatus()).isEqualTo(UserStatus.ACTIVE);

        assertThat(created.getPasswordHash())
                .isNotEqualTo("password123");

        assertThat(
                passwordEncoder.matches(
                        "password123",
                        created.getPasswordHash()))
                .isTrue();
    }

    @Test
    void register_should_reject_duplicate_email() {
        RegisterRequest request = validRegisterRequest();

        when(userRepository.existsByEmailIgnoreCase("john@example.com"))
                .thenReturn(true);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> authService.register(request));

        assertThat(exception.getStatus().value()).isEqualTo(409);
    }

    @Test
    void register_should_reject_short_password() {
        RegisterRequest request =
                new RegisterRequest(
                        "John",
                        "Doe",
                        LocalDate.of(2000, 1, 1),
                        "john@example.com",
                        "+261340000000",
                        "short");

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> authService.register(request));

        assertThat(exception.getStatus().value()).isEqualTo(400);
    }

    @Test
    void register_should_reject_future_birthdate() {
        RegisterRequest request =
                new RegisterRequest(
                        "John",
                        "Doe",
                        LocalDate.now().plusDays(1),
                        "john@example.com",
                        "+261340000000",
                        "password123");

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> authService.register(request));

        assertThat(exception.getStatus().value()).isEqualTo(400);
    }

    @Test
    void register_should_reject_invalid_email() {
        RegisterRequest request =
                new RegisterRequest(
                        "John",
                        "Doe",
                        LocalDate.of(2000, 1, 1),
                        "invalid-email",
                        "+261340000000",
                        "password123");

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> authService.register(request));

        assertThat(exception.getStatus().value()).isEqualTo(400);
    }

    @Test
    void authenticate_should_return_user_when_credentials_are_valid() {
        String password = "password123";

        UserEntity entity =
                userEntity(
                        passwordEncoder.encode(password),
                        UserStatusEntity.ACTIVE);

        when(userRepository.findByEmailIgnoreCase("john@example.com"))
                .thenReturn(Optional.of(entity));

        LoginRequest request =
                new LoginRequest(
                        " JOHN@EXAMPLE.COM ",
                        password);

        User authenticated = authService.authenticate(request);

        assertThat(authenticated.getId()).isEqualTo(entity.getId());
        assertThat(authenticated.getEmail()).isEqualTo("john@example.com");
        assertThat(authenticated.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void authenticate_should_reject_unknown_email() {
        when(userRepository.findByEmailIgnoreCase("john@example.com"))
                .thenReturn(Optional.empty());

        LoginRequest request =
                new LoginRequest(
                        "john@example.com",
                        "password123");

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> authService.authenticate(request));

        assertThat(exception.getStatus().value()).isEqualTo(401);
    }

    @Test
    void authenticate_should_reject_wrong_password() {
        UserEntity entity =
                userEntity(
                        passwordEncoder.encode("correct-password"),
                        UserStatusEntity.ACTIVE);

        when(userRepository.findByEmailIgnoreCase("john@example.com"))
                .thenReturn(Optional.of(entity));

        LoginRequest request =
                new LoginRequest(
                        "john@example.com",
                        "wrong-password");

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> authService.authenticate(request));

        assertThat(exception.getStatus().value()).isEqualTo(401);
    }

    @Test
    void authenticate_should_reject_disabled_user() {
        UserEntity entity =
                userEntity(
                        passwordEncoder.encode("password123"),
                        UserStatusEntity.DISABLED);

        when(userRepository.findByEmailIgnoreCase("john@example.com"))
                .thenReturn(Optional.of(entity));

        LoginRequest request =
                new LoginRequest(
                        "john@example.com",
                        "password123");

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> authService.authenticate(request));

        assertThat(exception.getStatus().value()).isEqualTo(401);
    }

    @Test
    void authenticate_should_reject_blank_password() {
        LoginRequest request =
                new LoginRequest(
                        "john@example.com",
                        " ");

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> authService.authenticate(request));

        assertThat(exception.getStatus().value()).isEqualTo(400);
    }

    private RegisterRequest validRegisterRequest() {
        return new RegisterRequest(
                "John",
                "Doe",
                LocalDate.of(2000, 1, 1),
                "john@example.com",
                "+261340000000",
                "password123");
    }

    private UserEntity userEntity(
            String passwordHash,
            UserStatusEntity status) {

        return UserEntity.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .birthdate(LocalDate.of(2000, 1, 1))
                .email("john@example.com")
                .phone("+261340000000")
                .passwordHash(passwordHash)
                .role(UserRoleEntity.CLIENT)
                .status(status)
                .build();
    }
}