package com.himanshu.portfolio_risk_analytics.service;

import com.himanshu.portfolio_risk_analytics.dto.AuthRequest;
import com.himanshu.portfolio_risk_analytics.dto.AuthResponse;
import com.himanshu.portfolio_risk_analytics.dto.OtpVerifyRequest;
import com.himanshu.portfolio_risk_analytics.dto.UserDto;
import com.himanshu.portfolio_risk_analytics.entity.User;
import com.himanshu.portfolio_risk_analytics.repository.UserRepository;
import com.himanshu.portfolio_risk_analytics.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class AuthService {
    private static final Logger logger = Logger.getLogger(AuthService.class.getName());
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailService = emailService;
    }

    public AuthResponse register(AuthRequest request) {
        // Check if email is already taken by a verified user
        userRepository.findByEmail(request.getEmail()).ifPresent(existingUser -> {
            if (existingUser.isEmailVerified()) {
                throw new RuntimeException("Email already registered");
            }
            // If unverified and OTP expired, remove the stale record to allow re-registration
            if (existingUser.getOtpExpiresAt() != null
                    && existingUser.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
                userRepository.delete(existingUser);
                logger.log(Level.INFO, "Removed stale unverified user: " + existingUser.getEmail());
            } else {
                throw new RuntimeException("OTP already sent. Please check your email or resend OTP.");
            }
        });

        String otp = generateOtp();

        User user = User.builder()
                .username(request.getEmail().split("@")[0] + "_" + java.util.UUID.randomUUID().toString().substring(0, 5))
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .otp(otp)
                .otpExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .emailVerified(false)
                .enabled(false)
                .build();

        userRepository.save(user);
        logger.log(Level.INFO, "User registered (pending OTP): " + user.getEmail());

        // Send OTP email
        emailService.sendOtpEmail(request.getEmail(), otp);

        return AuthResponse.builder()
                .message("Verification code sent to " + request.getEmail())
                .build();
    }

    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("No account found for this email"));

        if (user.isEmailVerified()) {
            throw new RuntimeException("Email is already verified. Please log in.");
        }

        if (user.getOtpExpiresAt() == null || user.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired. Please request a new one.");
        }

        if (!request.getOtp().equals(user.getOtp())) {
            throw new RuntimeException("Invalid OTP. Please try again.");
        }

        // Activate the user
        user.setEmailVerified(true);
        user.setEnabled(true);
        user.setOtp(null);
        user.setOtpExpiresAt(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        logger.log(Level.INFO, "Email verified for user: " + user.getEmail());
        return generateAuthResponse(user);
    }

    public AuthResponse resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found for this email"));

        if (user.isEmailVerified()) {
            throw new RuntimeException("Email is already verified. Please log in.");
        }

        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        emailService.sendOtpEmail(email, otp);
        logger.log(Level.INFO, "OTP resent to: " + email);

        return AuthResponse.builder()
                .message("New verification code sent to " + email)
                .build();
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.isEmailVerified()) {
            throw new RuntimeException("Please verify your email before logging in");
        }

        if (!user.isEnabled()) {
            throw new RuntimeException("User account is disabled");
        }

        logger.log(Level.INFO, "User logged in: " + user.getEmail());
        return generateAuthResponse(user);
    }

    private String generateOtp() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }

    private AuthResponse generateAuthResponse(User user) {
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(userDto)
                .expiresIn("24h")
                .build();
    }
}