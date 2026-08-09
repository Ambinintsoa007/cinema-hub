package hei.school.cinema.service;

import hei.school.cinema.exception.ApiException;
import hei.school.cinema.mapper.UserMapper;
import hei.school.cinema.model.User;
import hei.school.cinema.model.UserRole;
import hei.school.cinema.model.UserStatus;
import hei.school.cinema.repository.UserRepository;
import hei.school.cinema.repository.model.UserEntity;
import hei.school.cinema.repository.model.UserRoleEntity;
import hei.school.cinema.repository.model.UserStatusEntity;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public User getById(UUID userId) {
        return userMapper.toDomain(findEntity(userId));
    }

    @Transactional(readOnly = true)
    public Page<User> getAll(int page, int pageSize, UserRole role, UserStatus status) {
        validatePagination(page, pageSize);

        return userRepository
                .findAllFiltered(
                        userMapper.toEntityRole(role),
                        userMapper.toEntityStatus(status),
                        PageRequest.of(page, pageSize))
                .map(userMapper::toDomain);
    }

    @Transactional
    public User updateRole(UUID userId, UserRole newRole) {
        if (newRole == null) {
            throw ApiException.badRequest("User role must not be null");
        }

        UserEntity user = findEntity(userId);

        boolean demotingActiveManager =
                user.getRole() == UserRoleEntity.MANAGER
                        && user.getStatus() == UserStatusEntity.ACTIVE
                        && newRole != UserRole.MANAGER;

        if (demotingActiveManager && isLastActiveManager()) {
            throw ApiException.conflict("The last active manager cannot be demoted");
        }

        user.setRole(userMapper.toEntityRole(newRole));

        return userMapper.toDomain(userRepository.save(user));
    }

    @Transactional
    public User updateStatus(UUID userId, UserStatus newStatus) {
        if (newStatus == null) {
            throw ApiException.badRequest("User status must not be null");
        }

        UserEntity user = findEntity(userId);

        boolean disablingActiveManager =
                user.getRole() == UserRoleEntity.MANAGER
                        && user.getStatus() == UserStatusEntity.ACTIVE
                        && newStatus == UserStatus.DISABLED;

        if (disablingActiveManager && isLastActiveManager()) {
            throw ApiException.conflict("The last active manager cannot be disabled");
        }

        user.setStatus(userMapper.toEntityStatus(newStatus));

        return userMapper.toDomain(userRepository.save(user));
    }

    private UserEntity findEntity(UUID userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found: " + userId));
    }

    private boolean isLastActiveManager() {
        return userRepository.countByRoleAndStatus(
                UserRoleEntity.MANAGER, UserStatusEntity.ACTIVE)
                <= 1;
    }

    private void validatePagination(int page, int pageSize) {
        if (page < 0) {
            throw ApiException.badRequest("Page must be greater than or equal to 0");
        }

        if (pageSize < 1 || pageSize > 100) {
            throw ApiException.badRequest("Page size must be between 1 and 100");
        }
    }
}