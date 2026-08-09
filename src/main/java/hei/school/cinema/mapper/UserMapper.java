package hei.school.cinema.mapper;

import hei.school.cinema.endpoint.rest.dto.UserResponse;
import hei.school.cinema.model.User;
import hei.school.cinema.repository.model.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toDomain(UserEntity entity) {
        return User.builder()
                .id(entity.getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .birthdate(entity.getBirthdate())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .passwordHash(entity.getPasswordHash())
                .role(toDomainRole(entity.getRole()))
                .status(toDomainStatus(entity.getStatus()))
                .build();
    }

    public UserEntity toEntity(User user) {
        return UserEntity.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .birthdate(user.getBirthdate())
                .email(user.getEmail())
                .phone(user.getPhone())
                .passwordHash(user.getPasswordHash())
                .role(toEntityRole(user.getRole()))
                .status(toEntityStatus(user.getStatus()))
                .build();
    }

    public UserResponse toDto(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getBirthdate(),
                user.getEmail(),
                user.getPhone(),
                toDtoRole(user.getRole()),
                toDtoStatus(user.getStatus()));
    }

    public hei.school.cinema.model.UserRole toDomainRole(
            hei.school.cinema.endpoint.rest.dto.UserRole role) {
        if (role == null) {
            return null;
        }
        return hei.school.cinema.model.UserRole.valueOf(role.name());
    }

    public hei.school.cinema.model.UserStatus toDomainStatus(
            hei.school.cinema.endpoint.rest.dto.UserStatus status) {
        if (status == null) {
            return null;
        }
        return hei.school.cinema.model.UserStatus.valueOf(status.name());
    }

    public hei.school.cinema.model.UserRole toDomainRole(
            hei.school.cinema.repository.model.UserRoleEntity role) {
        if (role == null) {
            return null;
        }
        return hei.school.cinema.model.UserRole.valueOf(role.name());
    }

    public hei.school.cinema.model.UserStatus toDomainStatus(
            hei.school.cinema.repository.model.UserStatusEntity status) {
        if (status == null) {
            return null;
        }
        return hei.school.cinema.model.UserStatus.valueOf(status.name());
    }

    public hei.school.cinema.repository.model.UserRoleEntity toEntityRole(
            hei.school.cinema.model.UserRole role) {
        if (role == null) {
            return null;
        }
        return hei.school.cinema.repository.model.UserRoleEntity.valueOf(role.name());
    }

    public hei.school.cinema.repository.model.UserStatusEntity toEntityStatus(
            hei.school.cinema.model.UserStatus status) {
        if (status == null) {
            return null;
        }
        return hei.school.cinema.repository.model.UserStatusEntity.valueOf(status.name());
    }

    public hei.school.cinema.endpoint.rest.dto.UserRole toDtoRole(
            hei.school.cinema.model.UserRole role) {
        if (role == null) {
            return null;
        }
        return hei.school.cinema.endpoint.rest.dto.UserRole.valueOf(role.name());
    }

    public hei.school.cinema.endpoint.rest.dto.UserStatus toDtoStatus(
            hei.school.cinema.model.UserStatus status) {
        if (status == null) {
            return null;
        }
        return hei.school.cinema.endpoint.rest.dto.UserStatus.valueOf(status.name());
    }
}