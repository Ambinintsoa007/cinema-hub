package hei.school.cinema.endpoint.rest.controller;

import hei.school.cinema.endpoint.rest.dto.UpdateUserRoleRequest;
import hei.school.cinema.endpoint.rest.dto.UpdateUserStatusRequest;
import hei.school.cinema.endpoint.rest.dto.UserPageResponse;
import hei.school.cinema.endpoint.rest.dto.UserResponse;
import hei.school.cinema.endpoint.rest.dto.UserRole;
import hei.school.cinema.endpoint.rest.dto.UserStatus;
import hei.school.cinema.mapper.UserMapper;
import hei.school.cinema.model.User;
import hei.school.cinema.service.UserService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping
    public ResponseEntity<UserPageResponse> getUsers(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status) {

        Page<User> users =
                userService.getAll(
                        page,
                        pageSize,
                        userMapper.toDomainRole(role),
                        userMapper.toDomainStatus(status));

        List<UserResponse> data = users.stream().map(userMapper::toDto).toList();

        return ResponseEntity.ok(
                new UserPageResponse(
                        data,
                        page,
                        pageSize,
                        users.getTotalElements(),
                        users.getTotalPages()));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        return ResponseEntity.ok(userMapper.toDto(userService.getById(userId)));
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable UUID userId,
            @RequestBody UpdateUserRoleRequest request) {

        User updated =
                userService.updateRole(
                        userId,
                        userMapper.toDomainRole(request.getRole()));

        return ResponseEntity.ok(userMapper.toDto(updated));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable UUID userId,
            @RequestBody UpdateUserStatusRequest request) {

        User updated =
                userService.updateStatus(
                        userId,
                        userMapper.toDomainStatus(request.getStatus()));

        return ResponseEntity.ok(userMapper.toDto(updated));
    }
}