package hei.school.cinema.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import hei.school.cinema.endpoint.rest.dto.UserResponse;
import hei.school.cinema.model.User;
import hei.school.cinema.model.UserRole;
import hei.school.cinema.model.UserStatus;
import hei.school.cinema.repository.model.UserEntity;
import hei.school.cinema.repository.model.UserRoleEntity;
import hei.school.cinema.repository.model.UserStatusEntity;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserMapperTest {

  private final UserMapper userMapper = new UserMapper();

  @Test
  void toDomain_should_map_user_entity() {
    UUID id = UUID.randomUUID();

    UserEntity entity =
        UserEntity.builder()
            .id(id)
            .firstName("Tahiana")
            .lastName("Razafimamonjy")
            .birthdate(LocalDate.of(2000, 1, 1))
            .email("test@example.com")
            .phone("+261340000000")
            .passwordHash("hashed-password")
            .role(UserRoleEntity.MANAGER)
            .status(UserStatusEntity.ACTIVE)
            .build();

    User user = userMapper.toDomain(entity);

    assertThat(user.getId()).isEqualTo(id);
    assertThat(user.getFirstName()).isEqualTo("Tahiana");
    assertThat(user.getLastName()).isEqualTo("Razafimamonjy");
    assertThat(user.getBirthdate()).isEqualTo(LocalDate.of(2000, 1, 1));
    assertThat(user.getEmail()).isEqualTo("test@example.com");
    assertThat(user.getPhone()).isEqualTo("+261340000000");
    assertThat(user.getPasswordHash()).isEqualTo("hashed-password");
    assertThat(user.getRole()).isEqualTo(UserRole.MANAGER);
    assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
  }

  @Test
  void toEntity_should_map_domain_user() {
    UUID id = UUID.randomUUID();

    User user =
        User.builder()
            .id(id)
            .firstName("John")
            .lastName("Doe")
            .birthdate(LocalDate.of(1995, 5, 10))
            .email("john@example.com")
            .phone("+261330000000")
            .passwordHash("hashed-password")
            .role(UserRole.CLIENT)
            .status(UserStatus.ACTIVE)
            .build();

    UserEntity entity = userMapper.toEntity(user);

    assertThat(entity.getId()).isEqualTo(id);
    assertThat(entity.getRole()).isEqualTo(UserRoleEntity.CLIENT);
    assertThat(entity.getStatus()).isEqualTo(UserStatusEntity.ACTIVE);
    assertThat(entity.getPasswordHash()).isEqualTo("hashed-password");
  }

  @Test
  void toDto_should_not_expose_password() {
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .firstName("Jane")
            .lastName("Doe")
            .birthdate(LocalDate.of(1998, 2, 15))
            .email("jane@example.com")
            .phone("+261320000000")
            .passwordHash("secret-hash")
            .role(UserRole.EMPLOYEE)
            .status(UserStatus.ACTIVE)
            .build();

    UserResponse response = userMapper.toDto(user);

    assertThat(response.getFirstName()).isEqualTo("Jane");
    assertThat(response.getRole()).isEqualTo(hei.school.cinema.endpoint.rest.dto.UserRole.EMPLOYEE);
    assertThat(response.getStatus())
        .isEqualTo(hei.school.cinema.endpoint.rest.dto.UserStatus.ACTIVE);
  }

  @Test
  void enum_mappings_should_handle_null() {
    assertThat(userMapper.toDomainRole((hei.school.cinema.endpoint.rest.dto.UserRole) null))
        .isNull();

    assertThat(userMapper.toDomainStatus((hei.school.cinema.endpoint.rest.dto.UserStatus) null))
        .isNull();

    assertThat(userMapper.toEntityRole(null)).isNull();

    assertThat(userMapper.toEntityStatus(null)).isNull();
  }
}
