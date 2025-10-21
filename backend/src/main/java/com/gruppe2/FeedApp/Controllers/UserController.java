package com.gruppe2.FeedApp.controllers;



import org.springframework.web.bind.annotation.CrossOrigin;

import org.springframework.web.bind.annotation.*;

import com.gruppe2.FeedApp.domain.User;
import com.gruppe2.FeedApp.services.PollManager;

import java.util.Collection;

@CrossOrigin
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final PollManager manager;
    public UserController(PollManager manager) { this.manager = manager; }

    @PostMapping
    public User create(@RequestBody CreateUserRequest req) {
        return manager.registerUser(req.username, req.password, req.email);
    }

    @PostMapping("/login")
    public User login(@RequestBody CreateUserRequest req) {
        return manager.loginUser(req.username, req.password)
            .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));
    }

    @GetMapping
    public Collection<User> list() { return manager.allUsers(); }

    @GetMapping("/{userId}")
    public User get(@PathVariable Long userId) {
        return manager.findUser(userId).orElseThrow();
    }

    @PutMapping("/{userId}")
    public User update(@PathVariable Long userId, @RequestBody CreateUserRequest req) {
        return manager.registerUser(req.username, req.password, req.email);
    }

    @DeleteMapping("/{userId}")
    public void delete(@PathVariable Long userId) {
    }

    public static class CreateUserRequest {
        public String username;
        public String password;
        public String email;
    }
}

