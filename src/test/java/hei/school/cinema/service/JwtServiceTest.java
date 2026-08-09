package hei.school.cinema.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hei.school.cinema.model.User;
import hei.school.cinema.model.UserRole;
import hei.school.cinema.model.UserStatus;
import io.jsonwebtoken.JwtException;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET =
            "test-secret-key-that-is-long-enough-for-jwt-tests-123456789";

    @Test
    void generateToken_should_create_valid_token() {
        JwtService jwtService = new JwtService(SECRET, 3600);
        User user = user();

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void extractUserId_should_return_token_subject() {
        JwtService jwtService = new JwtService(SECRET, 3600);
        User user = user();

        String token = jwtService.generateToken(user);

        UUID extractedUserId = jwtService.extractUserId(token);

        assertThat(extractedUserId).isEqualTo(user.getId());
    }

    @Test
    void isValid_should_return_false_for_invalid_token() {
        JwtService jwtService = new JwtService(SECRET, 3600);

        boolean valid = jwtService.isValid("invalid-token");

        assertThat(valid).isFalse();
    }

    @Test
    void extractUserId_should_reject_invalid_token() {
        JwtService jwtService = new JwtService(SECRET, 3600);

        assertThrows(
                JwtException.class,
                () -> jwtService.extractUserId("invalid-token"));
    }

    @Test
    void token_signed_with_another_secret_should_be_invalid() {
        JwtService jwtService = new JwtService(SECRET, 3600);

        JwtService anotherJwtService =
                new JwtService(
                        "another-test-secret-key-that-is-long-enough-987654321",
                        3600);

        String token = jwtService.generateToken(user());

        assertThat(anotherJwtService.isValid(token)).isFalse();
    }

    @Test
    void expired_token_should_be_invalid() {
        JwtService jwtService = new JwtService(SECRET, -1);

        String token = jwtService.generateToken(user());

        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void getExpirationSeconds_should_return_configured_value() {
        JwtService jwtService = new JwtService(SECRET, 3600);

        assertThat(jwtService.getExpirationSeconds()).isEqualTo(3600);
    }

    private User user() {
        return User.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .birthdate(LocalDate.of(2000, 1, 1))
                .email("john@example.com")
                .phone("+261340000000")
                .passwordHash("hashed-password")
                .role(UserRole.CLIENT)
                .status(UserStatus.ACTIVE)
                .build();
    }
}