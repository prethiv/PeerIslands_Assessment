package com.peerisland.ecomm.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles user self-registration. The signup endpoint is left public in
 * {@link SecurityConfig}; all other endpoints require authentication.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user. Returns 200 with a confirmation message, or 409 if
     * the username is already taken / 400 for invalid input.
     */
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest request) {
        String username = request.username() == null ? "" : request.username().trim();
        String password = request.password() == null ? "" : request.password();

        if (username.isEmpty() || password.isEmpty()) {
            return ResponseEntity.badRequest().body("Username and password are required.");
        }
        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username '" + username + "' is already taken.");
        }

        User user = new User(username, passwordEncoder.encode(password));
        userRepository.save(user);
        return ResponseEntity.ok("User '" + username + "' registered successfully. You can now sign in.");
    }

    /**
     * Request payload for {@link #signup(SignupRequest)}.
     */
    public record SignupRequest(String username, String password) {
    }
}
