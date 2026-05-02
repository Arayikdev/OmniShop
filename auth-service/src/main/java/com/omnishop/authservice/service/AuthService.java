package com.omnishop.authservice.service;

import com.omnishop.authservice.dto.LoginRequest;
import com.omnishop.authservice.dto.LoginResponse;
import com.omnishop.authservice.dto.RegisterRequest;
import com.omnishop.authservice.entity.User;
import com.omnishop.authservice.entity.UserRole;
import com.omnishop.authservice.exception.InvalidCredentialsException;
import com.omnishop.authservice.exception.UsernameAlreadyExistsException;
import com.omnishop.authservice.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public void register(RegisterRequest request, UserRole role) {
        String username = request.getUsername().trim().toLowerCase();
        if (userRepository.findByUsername(username).isPresent()) {
            throw new UsernameAlreadyExistsException("Username already exists: " + username);
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername().trim().toLowerCase();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }
        String token = jwtUtil.generateToken(user);
        return new LoginResponse(token, user.getId(), user.getRole().name(), 86400);
    }
}
