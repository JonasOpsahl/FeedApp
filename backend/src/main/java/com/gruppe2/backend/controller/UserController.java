package com.gruppe2.backend.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gruppe2.backend.model.User;
import com.gruppe2.backend.service.PollService;


@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private PollService pollService;

    public UserController(PollService pollService) {
        this.pollService = pollService;
    }

    @RequestMapping
    public List<User> getUsers() {
        return pollService.getUsers();
    }

    @RequestMapping("/{id}")
    public User getUser(@PathVariable Integer id) {
        return pollService.getUser(id);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Integer id,
                                           @RequestParam Optional<String> username,
                                           @RequestParam Optional<String> email,
                                           @RequestParam Optional<String> password,
                                           @AuthenticationPrincipal User authenticatedUser) {

        if (authenticatedUser == null || !authenticatedUser.getUserId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User updatedUser = pollService.updateUser(id, username, email, password);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteUser(@PathVariable Integer id,
                                              @AuthenticationPrincipal User authenticatedUser) {

        if (authenticatedUser == null || !authenticatedUser.getUserId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(false);
        }

        boolean deleted = pollService.deleteUser(id);
        return ResponseEntity.ok(deleted);
    }
}
