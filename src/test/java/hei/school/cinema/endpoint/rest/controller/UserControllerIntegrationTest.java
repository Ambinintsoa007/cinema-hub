package hei.school.cinema.endpoint.rest.controller;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import hei.school.cinema.endpoint.rest.dto.UpdateUserRoleRequest;
import hei.school.cinema.endpoint.rest.dto.UpdateUserStatusRequest;
import hei.school.cinema.endpoint.rest.dto.UserRole;
import hei.school.cinema.endpoint.rest.dto.UserStatus;
import hei.school.cinema.mapper.UserMapper;
import hei.school.cinema.repository.UserRepository;
import hei.school.cinema.repository.model.UserEntity;
import hei.school.cinema.repository.model.UserRoleEntity;
import hei.school.cinema.repository.model.UserStatusEntity;
import hei.school.cinema.service.JwtService;
import java.time.LocalDate;
import java.util.List;
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
class UserControllerIntegrationTest {

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
    void getUsers_should_return_paginated_users() throws Exception {
        String token = managerToken();

        saveUser(
                "client.one@example.com",
                UserRoleEntity.CLIENT,
                UserStatusEntity.ACTIVE);

        mockMvc
                .perform(
                        get("/users")
                                .param("page", "0")
                                .param("pageSize", "20")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    void getUsers_should_filter_by_role_and_status() throws Exception {
        String token = managerToken();

        saveUser(
                "active.client@example.com",
                UserRoleEntity.CLIENT,
                UserStatusEntity.ACTIVE);

        saveUser(
                "disabled.client@example.com",
                UserRoleEntity.CLIENT,
                UserStatusEntity.DISABLED);

        saveUser(
                "employee@example.com",
                UserRoleEntity.EMPLOYEE,
                UserStatusEntity.ACTIVE);

        mockMvc
                .perform(
                        get("/users")
                                .param("role", "CLIENT")
                                .param("status", "ACTIVE")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[*].role", everyItem(is("CLIENT"))))
                .andExpect(jsonPath("$.data[*].status", everyItem(is("ACTIVE"))));
    }

    @Test
    void getUsers_should_reject_invalid_pagination() throws Exception {
        String token = managerToken();

        mockMvc
                .perform(
                        get("/users")
                                .param("page", "-1")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        mockMvc
                .perform(
                        get("/users")
                                .param("pageSize", "101")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserById_should_return_user() throws Exception {
        String token = managerToken();

        UserEntity client =
                saveUser(
                        "client.details@example.com",
                        UserRoleEntity.CLIENT,
                        UserStatusEntity.ACTIVE);

        mockMvc
                .perform(
                        get("/users/{userId}", client.getId())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(client.getId().toString()))
                .andExpect(jsonPath("$.email").value("client.details@example.com"))
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void getUserById_should_return_404_when_user_does_not_exist()
            throws Exception {

        String token = managerToken();

        mockMvc
                .perform(
                        get("/users/{userId}", UUID.randomUUID())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("NOT_FOUND"));
    }

    @Test
    void updateRole_should_change_user_role() throws Exception {
        String token = managerToken();

        UserEntity client =
                saveUser(
                        "role.client@example.com",
                        UserRoleEntity.CLIENT,
                        UserStatusEntity.ACTIVE);

        UpdateUserRoleRequest request =
                new UpdateUserRoleRequest(UserRole.EMPLOYEE);

        mockMvc
                .perform(
                        patch("/users/{userId}/role", client.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    void updateStatus_should_disable_user() throws Exception {
        String token = managerToken();

        UserEntity client =
                saveUser(
                        "status.client@example.com",
                        UserRoleEntity.CLIENT,
                        UserStatusEntity.ACTIVE);

        UpdateUserStatusRequest request =
                new UpdateUserStatusRequest(UserStatus.DISABLED);

        mockMvc
                .perform(
                        patch("/users/{userId}/status", client.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    void updateRole_should_reject_demoting_last_active_manager()
            throws Exception {

        disableExistingActiveManagers();

        UserEntity manager =
                saveUser(
                        "last.manager.role@example.com",
                        UserRoleEntity.MANAGER,
                        UserStatusEntity.ACTIVE);

        String token =
                jwtService.generateToken(userMapper.toDomain(manager));

        UpdateUserRoleRequest request =
                new UpdateUserRoleRequest(UserRole.CLIENT);

        mockMvc
                .perform(
                        patch("/users/{userId}/role", manager.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("CONFLICT"));
    }

    @Test
    void updateStatus_should_reject_disabling_last_active_manager()
            throws Exception {

        disableExistingActiveManagers();

        UserEntity manager =
                saveUser(
                        "last.manager.status@example.com",
                        UserRoleEntity.MANAGER,
                        UserStatusEntity.ACTIVE);

        String token =
                jwtService.generateToken(userMapper.toDomain(manager));

        UpdateUserStatusRequest request =
                new UpdateUserStatusRequest(UserStatus.DISABLED);

        mockMvc
                .perform(
                        patch("/users/{userId}/status", manager.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("CONFLICT"));
    }

    @Test
    void updateRole_should_allow_demoting_manager_when_another_manager_exists()
            throws Exception {

        UserEntity authenticatedManager =
                saveUser(
                        "manager.one@example.com",
                        UserRoleEntity.MANAGER,
                        UserStatusEntity.ACTIVE);

        UserEntity targetManager =
                saveUser(
                        "manager.two@example.com",
                        UserRoleEntity.MANAGER,
                        UserStatusEntity.ACTIVE);

        String token =
                jwtService.generateToken(userMapper.toDomain(authenticatedManager));

        UpdateUserRoleRequest request =
                new UpdateUserRoleRequest(UserRole.EMPLOYEE);

        mockMvc
                .perform(
                        patch("/users/{userId}/role", targetManager.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    private String managerToken() {
        UserEntity manager =
                saveUser(
                        UUID.randomUUID() + "@manager.test",
                        UserRoleEntity.MANAGER,
                        UserStatusEntity.ACTIVE);

        return jwtService.generateToken(userMapper.toDomain(manager));
    }

    private UserEntity saveUser(
            String email,
            UserRoleEntity role,
            UserStatusEntity status) {

        UserEntity user =
                UserEntity.builder()
                        .id(UUID.randomUUID())
                        .firstName("Test")
                        .lastName("User")
                        .birthdate(LocalDate.of(2000, 1, 1))
                        .email(email)
                        .phone("+261340000000")
                        .passwordHash(passwordEncoder.encode("password123"))
                        .role(role)
                        .status(status)
                        .build();

        return userRepository.save(user);
    }

    private void disableExistingActiveManagers() {
        List<UserEntity> activeManagers =
                userRepository.findAll().stream()
                        .filter(user -> user.getRole() == UserRoleEntity.MANAGER)
                        .filter(user -> user.getStatus() == UserStatusEntity.ACTIVE)
                        .toList();

        activeManagers.forEach(
                user -> user.setStatus(UserStatusEntity.DISABLED));

        userRepository.saveAll(activeManagers);
        userRepository.flush();
    }
}