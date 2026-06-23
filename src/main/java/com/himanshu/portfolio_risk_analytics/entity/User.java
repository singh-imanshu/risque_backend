package com.himanshu.portfolio_risk_analytics.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User {
    @Id
    private String id;

    @NotBlank(message = "Username cannot be blank")
    private String username;

    @Email(message = "Email must be valid")
    @NotBlank(message = "Email cannot be blank")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    private String password;

    private String firstName;
    private String lastName;

    // OTP verification fields
    private String otp;
    private LocalDateTime otpExpiresAt;

    @Builder.Default
    private boolean emailVerified = false;

    @Builder.Default
    private boolean enabled = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}