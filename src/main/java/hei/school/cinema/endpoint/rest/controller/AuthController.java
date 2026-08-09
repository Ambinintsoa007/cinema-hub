package hei.school.cinema.endpoint.rest.controller;

import hei.school.cinema.endpoint.rest.dto.AuthResponse;
import hei.school.cinema.endpoint.rest.dto.LoginRequest;
import hei.school.cinema.endpoint.rest.dto.RegisterRequest;
import hei.school.cinema.endpoint.rest.dto.UserResponse;
import hei.school.cinema.mapper.UserMapper;
import hei.school.cinema.model.User;
import hei.school.cinema.service.AuthService;
import hei.school.cinema.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest registerRequest) {
        User registeredUser = authService.register(registerRequest);

        return ResponseEntity.status(201).body(userMapper.toDto(registeredUser));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        User authenticatedUser = authService.authenticate(loginRequest);

        String accessToken = jwtService.generateToken(authenticatedUser);

        return ResponseEntity.ok(
                new AuthResponse(accessToken, "Bearer", jwtService.getExpirationSeconds()));
    }
}