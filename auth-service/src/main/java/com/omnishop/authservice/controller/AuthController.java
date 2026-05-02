package com.omnishop.authservice.controller;

import com.omnishop.authservice.dto.LoginRequest;
import com.omnishop.authservice.dto.LoginResponse;
import com.omnishop.authservice.dto.RegisterRequest;
import com.omnishop.authservice.entity.UserRole;
import com.omnishop.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Register and login endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register as customer",
               description = "Creates a new customer account. Role is automatically assigned as ROLE_CUSTOMER.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Account created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error or username already exists"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request, UserRole.ROLE_CUSTOMER);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/register/seller")
    @Operation(summary = "Register as seller",
               description = "Creates a new seller account. Role is automatically assigned as ROLE_SELLER.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Seller account created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error or username already exists"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> registerSeller(@Valid @RequestBody RegisterRequest request) {
        authService.register(request, UserRole.ROLE_SELLER);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    @Operation(summary = "Login and obtain JWT",
               description = "Authenticates the user and returns a signed JWT token valid for 24 hours.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful, JWT token returned"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
