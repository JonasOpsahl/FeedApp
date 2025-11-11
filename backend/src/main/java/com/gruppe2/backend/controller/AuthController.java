package com.gruppe2.backend.controller;

import com.gruppe2.backend.config.security.JwtService;
import com.gruppe2.backend.model.User;
import com.gruppe2.backend.service.PollService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

record LoginRequest(String username, String password) {}
record LoginResponse(String token, Integer userId, String username) {}
record RegisterRequest(String username, String email, String password) {}

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PollService pollService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService, PollService pollService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.pollService = pollService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);

        User user = (User) userDetails;
        LoginResponse response = new LoginResponse(token, user.getUserId(), user.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterRequest request) {
        User newUser = pollService.createUser(
                request.username(),
                request.email(),
                request.password()
        );
        return ResponseEntity.ok(newUser);
    }
}
