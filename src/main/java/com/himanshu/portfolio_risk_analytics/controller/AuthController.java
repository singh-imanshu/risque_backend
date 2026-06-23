package com.himanshu.portfolio_risk_analytics.controller;

import com.himanshu.portfolio_risk_analytics.dto.ApiResponse;
import com.himanshu.portfolio_risk_analytics.dto.AuthRequest;
import com.himanshu.portfolio_risk_analytics.dto.AuthResponse;
import com.himanshu.portfolio_risk_analytics.dto.OtpVerifyRequest;
import com.himanshu.portfolio_risk_analytics.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    private static final Logger logger = Logger.getLogger(AuthController.class.getName());
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody AuthRequest request) {
        try {
            AuthResponse response = authService.register(request);
            logger.log(Level.INFO, "Registration initiated: " + request.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "Verification code sent"));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Registration failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "REGISTRATION_FAILED"));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        try {
            AuthResponse response = authService.verifyOtp(request);
            logger.log(Level.INFO, "Email verified: " + request.getEmail());
            return ResponseEntity.ok(ApiResponse.success(response, "Email verified successfully"));
        } catch (Exception e) {
            logger.log(Level.WARNING, "OTP verification failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "OTP_VERIFICATION_FAILED"));
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> resendOtp(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Email is required", "INVALID_INPUT"));
            }
            AuthResponse response = authService.resendOtp(email);
            logger.log(Level.INFO, "OTP resent to: " + email);
            return ResponseEntity.ok(ApiResponse.success(response, "Verification code resent"));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Resend OTP failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "RESEND_OTP_FAILED"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        try {
            AuthResponse response = authService.login(request);
            logger.log(Level.INFO, "User logged in: " + request.getEmail());
            return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Login failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), "LOGIN_FAILED"));
        }
    }
}