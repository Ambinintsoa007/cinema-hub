package hei.school.cinema.endpoint.rest.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import hei.school.cinema.endpoint.rest.dto.LoginRequest;
import hei.school.cinema.endpoint.rest.dto.RegisterRequest;
import hei.school.cinema.mapper.UserMapper;
import hei.school.cinema.model.User;
import hei.school.cinema.repository.UserRepository;
import hei.school.cinema.repository.model.UserEntity;
import hei.school.cinema.repository.model.UserRoleEntity;
import hei.school.cinema.repository.model.UserStatusEntity;
import hei.school.cinema.service.JwtService;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
class AuthControllerIntegrationTest {

  @Container
  static final PostgreSQLContainer POSTGRESQL =
      new PostgreSQLContainer("postgres:16-alpine")
          .withDatabaseName("cinema_test")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void configureDatabase(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRESQL::getUsername);
    registry.add("spring.datasource.password", POSTGRESQL::getPassword);
  }

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private UserRepository userRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private JwtService jwtService;

  @Autowired private UserMapper userMapper;

  @Test
  void register_should_create_active_client() throws Exception {
    RegisterRequest request =
        new RegisterRequest(
            "John",
            "Doe",
            LocalDate.of(2000, 1, 1),
            "john.register@example.com",
            "+261340000000",
            "password123");

    mockMvc
        .perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.firstName").value("John"))
        .andExpect(jsonPath("$.lastName").value("Doe"))
        .andExpect(jsonPath("$.email").value("john.register@example.com"))
        .andExpect(jsonPath("$.role").value("CLIENT"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
  }

  @Test
  void register_should_reject_duplicate_email() throws Exception {
    RegisterRequest request =
        new RegisterRequest(
            "John",
            "Doe",
            LocalDate.of(2000, 1, 1),
            "duplicate@example.com",
            "+261340000000",
            "password123");

    mockMvc.perform(
        post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

    request.setEmail("DUPLICATE@EXAMPLE.COM");

    mockMvc
        .perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.type").value("CONFLICT"));
  }

  @Test
  void login_should_return_bearer_token() throws Exception {
    String password = "password123";

    saveUser("login@example.com", password, UserRoleEntity.CLIENT, UserStatusEntity.ACTIVE);

    LoginRequest request = new LoginRequest("login@example.com", password);

    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").value(3600));
  }

  @Test
  void login_should_reject_wrong_password() throws Exception {
    saveUser(
        "wrong.password@example.com",
        "correct-password",
        UserRoleEntity.CLIENT,
        UserStatusEntity.ACTIVE);

    LoginRequest request = new LoginRequest("wrong.password@example.com", "wrong-password");

    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.type").value("UNAUTHORIZED"));
  }

  @Test
  void login_should_reject_disabled_user() throws Exception {
    saveUser(
        "disabled@example.com", "password123", UserRoleEntity.CLIENT, UserStatusEntity.DISABLED);

    LoginRequest request = new LoginRequest("disabled@example.com", "password123");

    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void users_should_return_401_without_token() throws Exception {
    mockMvc
        .perform(get("/users"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.type").value("UNAUTHORIZED"));
  }

  @Test
  void users_should_return_403_for_client() throws Exception {
    UserEntity client =
        saveUser(
            "client@example.com", "password123", UserRoleEntity.CLIENT, UserStatusEntity.ACTIVE);

    String token = jwtService.generateToken(userMapper.toDomain(client));

    mockMvc
        .perform(get("/users").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.type").value("FORBIDDEN"));
  }

  @Test
  void users_should_be_accessible_for_manager() throws Exception {
    UserEntity manager =
        saveUser(
            "manager@example.com", "password123", UserRoleEntity.MANAGER, UserStatusEntity.ACTIVE);

    String token = jwtService.generateToken(userMapper.toDomain(manager));

    mockMvc
        .perform(get("/users").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray());
  }

  @Test
  void users_should_reject_invalid_token() throws Exception {
    mockMvc
        .perform(get("/users").header("Authorization", "Bearer invalid-token"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void disabled_user_token_should_no_longer_authenticate() throws Exception {
    UserEntity client =
        saveUser(
            "disabled.token@example.com",
            "password123",
            UserRoleEntity.CLIENT,
            UserStatusEntity.ACTIVE);

    User user = userMapper.toDomain(client);
    String token = jwtService.generateToken(user);

    client.setStatus(UserStatusEntity.DISABLED);
    userRepository.save(client);

    mockMvc
        .perform(get("/users").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  private UserEntity saveUser(
      String email, String password, UserRoleEntity role, UserStatusEntity status) {

    UserEntity entity =
        UserEntity.builder()
            .id(UUID.randomUUID())
            .firstName("Test")
            .lastName("User")
            .birthdate(LocalDate.of(2000, 1, 1))
            .email(email)
            .phone("+261340000000")
            .passwordHash(passwordEncoder.encode(password))
            .role(role)
            .status(status)
            .build();

    return userRepository.save(entity);
  }
}
