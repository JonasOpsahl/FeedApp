package com.gruppe2.backend.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gruppe2.backend.model.User;
import com.gruppe2.backend.service.PollService;


@RestController
@CrossOrigin
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

    @PostMapping
    public User createUser(@RequestParam String username, @RequestParam String email, @RequestParam String password) {
        return pollService.createUser(username, email, password);
    }
    
    @PutMapping("/{id}")
    public User updateUser(@PathVariable Integer id, @RequestParam Optional<String> username, @RequestParam Optional<String> email, @RequestParam Optional<String> password) {
        return pollService.updateUser(id, username, email, password);
    }

    @DeleteMapping("/{id}")
    public boolean deleteUser(@PathVariable Integer id) {
        return pollService.deleteUser(id);
    }

    @PostMapping("/{id}/login")
    public String loginUser(@PathVariable Integer id) {

        pollService.loginUser(id);
        return "User " + id + " logged in.";
    }

    @PostMapping("/{id}/logout")
    public String logoutUser(@PathVariable Integer id) {
        pollService.logoutUser(id);
        return "User " + id + " logged out.";
    }

    @GetMapping("/{id}/isloggedin")
    public boolean isUserLoggedIn(@PathVariable Integer id) {
        return pollService.isUserLoggedIn(id);
    }

    @GetMapping("/loggedin")
    public Set<String> getLoggedInUsers() {
        return pollService.getLoggedInUsers();
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyUser(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        var users = pollService.getUsers();
        for (var user : users) {
            if (user.getUsername().equals(username)) {
                boolean passwordMatches = user.getPassword().equals(password);
                if (passwordMatches) {
                    return ResponseEntity.ok(Map.of(
                        "valid", true,
                        "userId", user.getUserId(),
                        "username", user.getUsername()
                    ));
                } else {
                    return ResponseEntity.status(401).body(Map.of(
                        "valid", false,
                        "error", "Incorrect password"
                    ));
                }
            }
        }

        return ResponseEntity.status(404).body(Map.of(
            "valid", false,
            "error", "User not found"
        ));
    }
}
