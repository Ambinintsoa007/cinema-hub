package hei.school.cinema.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;

  private UserService userService;

  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @BeforeEach
  void setUp() {
    userService = new UserService(userRepository, new UserMapper());
  }

  @Test
  void getById_should_return_user() {
    when(userRepository.findById(USER_ID))
        .thenReturn(Optional.of(userEntity(UserRoleEntity.CLIENT, UserStatusEntity.ACTIVE)));

    User user = userService.getById(USER_ID);

    assertThat(user.getId()).isEqualTo(USER_ID);
    assertThat(user.getRole()).isEqualTo(UserRole.CLIENT);
    assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
  }

  @Test
  void getById_should_return_404_when_user_does_not_exist() {
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    ApiException exception = assertThrows(ApiException.class, () -> userService.getById(USER_ID));

    assertThat(exception.getStatus().value()).isEqualTo(404);
  }

  @Test
  void getAll_should_return_paginated_users() {
    UserEntity entity = userEntity(UserRoleEntity.CLIENT, UserStatusEntity.ACTIVE);

    when(userRepository.findAllFiltered(
            any(UserRoleEntity.class), any(UserStatusEntity.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(entity)));

    Page<User> result = userService.getAll(0, 20, UserRole.CLIENT, UserStatus.ACTIVE);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getId()).isEqualTo(USER_ID);
  }

  @Test
  void getAll_should_reject_negative_page() {
    ApiException exception =
        assertThrows(ApiException.class, () -> userService.getAll(-1, 20, null, null));

    assertThat(exception.getStatus().value()).isEqualTo(400);
  }

  @Test
  void getAll_should_reject_page_size_below_one() {
    ApiException exception =
        assertThrows(ApiException.class, () -> userService.getAll(0, 0, null, null));

    assertThat(exception.getStatus().value()).isEqualTo(400);
  }

  @Test
  void getAll_should_reject_page_size_above_one_hundred() {
    ApiException exception =
        assertThrows(ApiException.class, () -> userService.getAll(0, 101, null, null));

    assertThat(exception.getStatus().value()).isEqualTo(400);
  }

  @Test
  void updateRole_should_change_role() {
    UserEntity entity = userEntity(UserRoleEntity.CLIENT, UserStatusEntity.ACTIVE);

    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(entity));
    when(userRepository.save(any(UserEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    User updated = userService.updateRole(USER_ID, UserRole.EMPLOYEE);

    assertThat(updated.getRole()).isEqualTo(UserRole.EMPLOYEE);
  }

  @Test
  void updateRole_should_reject_null_role() {
    ApiException exception =
        assertThrows(ApiException.class, () -> userService.updateRole(USER_ID, null));

    assertThat(exception.getStatus().value()).isEqualTo(400);
  }

  @Test
  void updateRole_should_reject_demoting_last_active_manager() {
    UserEntity manager = userEntity(UserRoleEntity.MANAGER, UserStatusEntity.ACTIVE);

    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(manager));
    when(userRepository.countByRoleAndStatus(UserRoleEntity.MANAGER, UserStatusEntity.ACTIVE))
        .thenReturn(1L);

    ApiException exception =
        assertThrows(ApiException.class, () -> userService.updateRole(USER_ID, UserRole.CLIENT));

    assertThat(exception.getStatus().value()).isEqualTo(409);
  }

  @Test
  void updateRole_should_allow_demoting_manager_when_another_active_manager_exists() {
    UserEntity manager = userEntity(UserRoleEntity.MANAGER, UserStatusEntity.ACTIVE);

    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(manager));
    when(userRepository.countByRoleAndStatus(UserRoleEntity.MANAGER, UserStatusEntity.ACTIVE))
        .thenReturn(2L);
    when(userRepository.save(any(UserEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    User updated = userService.updateRole(USER_ID, UserRole.EMPLOYEE);

    assertThat(updated.getRole()).isEqualTo(UserRole.EMPLOYEE);
  }

  @Test
  void updateStatus_should_disable_client() {
    UserEntity client = userEntity(UserRoleEntity.CLIENT, UserStatusEntity.ACTIVE);

    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(client));
    when(userRepository.save(any(UserEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    User updated = userService.updateStatus(USER_ID, UserStatus.DISABLED);

    assertThat(updated.getStatus()).isEqualTo(UserStatus.DISABLED);
  }

  @Test
  void updateStatus_should_reject_null_status() {
    ApiException exception =
        assertThrows(ApiException.class, () -> userService.updateStatus(USER_ID, null));

    assertThat(exception.getStatus().value()).isEqualTo(400);
  }

  @Test
  void updateStatus_should_reject_disabling_last_active_manager() {
    UserEntity manager = userEntity(UserRoleEntity.MANAGER, UserStatusEntity.ACTIVE);

    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(manager));
    when(userRepository.countByRoleAndStatus(UserRoleEntity.MANAGER, UserStatusEntity.ACTIVE))
        .thenReturn(1L);

    ApiException exception =
        assertThrows(
            ApiException.class, () -> userService.updateStatus(USER_ID, UserStatus.DISABLED));

    assertThat(exception.getStatus().value()).isEqualTo(409);
  }

  @Test
  void updateStatus_should_allow_disabling_manager_when_another_active_manager_exists() {
    UserEntity manager = userEntity(UserRoleEntity.MANAGER, UserStatusEntity.ACTIVE);

    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(manager));
    when(userRepository.countByRoleAndStatus(UserRoleEntity.MANAGER, UserStatusEntity.ACTIVE))
        .thenReturn(2L);
    when(userRepository.save(any(UserEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    User updated = userService.updateStatus(USER_ID, UserStatus.DISABLED);

    assertThat(updated.getStatus()).isEqualTo(UserStatus.DISABLED);
  }

  private UserEntity userEntity(UserRoleEntity role, UserStatusEntity status) {

    return UserEntity.builder()
        .id(USER_ID)
        .firstName("John")
        .lastName("Doe")
        .birthdate(LocalDate.of(2000, 1, 1))
        .email("john@example.com")
        .phone("+261340000000")
        .passwordHash("hashed-password")
        .role(role)
        .status(status)
        .build();
  }
}
