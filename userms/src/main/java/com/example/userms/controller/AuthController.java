package com.example.userms.controller;

import com.example.userms.dto.ErrorResponse;
import com.example.userms.dto.LoginRequest;
import com.example.userms.dto.LoginResponse;
import com.example.userms.dto.RegisterRequest;
import com.example.userms.entity.User;
import com.example.userms.repository.UserRepository;
import com.example.userms.security.JwtUtil;
import com.example.userms.service.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final Map<String, String> verificationTokens = new ConcurrentHashMap<>();



    public AuthController(UserRepository userRepository,
                          JwtUtil jwtUtil,
                          BCryptPasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }



    // 🔐 User Registration Endpoint
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already registered");
        }

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setVerified(false);

        userRepository.save(newUser);

        String token = UUID.randomUUID().toString();
        verificationTokens.put(token, newUser.getEmail());
        emailService.sendVerificationEmail(newUser.getEmail(), token);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("User registered. Please check your email to verify your account.");
    }

    // 🔐 Login Endpoint with JWT
    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody LoginRequest request) {
        return userRepository.findByUsername(request.getUsername())
                .filter(User::isVerified)
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPassword()))
                .<ResponseEntity<Object>>map(user ->
                        ResponseEntity.ok(new LoginResponse(jwtUtil.generateToken(user.getUsername()))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Invalid credentials or unverified account", 401)));
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        String email = verificationTokens.remove(token);
        if (email == null) {
            return ResponseEntity.badRequest().body("Invalid or expired token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setVerified(true);
        userRepository.save(user);

        return ResponseEntity.ok("Email verified successfully. You can now log in.");
    }
}
