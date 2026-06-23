package com.himanshu.portfolio_risk_analytics.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class EmailService {
    private static final Logger logger = Logger.getLogger(EmailService.class.getName());
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("RISQUÉ — Verify Your Email");
            helper.setText(buildOtpHtml(otp), true);

            mailSender.send(message);
            logger.log(Level.INFO, "OTP email sent to: " + toEmail);
        } catch (MessagingException e) {
            logger.log(Level.SEVERE, "Failed to send OTP email to: " + toEmail, e);
            throw new RuntimeException("Failed to send verification email. Please try again.");
        }
    }

    private String buildOtpHtml(String otp) {
        return """
                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 480px; margin: 0 auto; padding: 32px;">
                  <div style="text-align: center; margin-bottom: 24px;">
                    <h1 style="color: #6366f1; font-size: 28px; margin: 0;">RISQUÉ</h1>
                    <p style="color: #64748b; font-size: 14px; margin: 4px 0 0;">Portfolio Risk Analytics</p>
                  </div>
                  <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 32px; text-align: center;">
                    <p style="color: #334155; font-size: 16px; margin: 0 0 8px;">Your verification code is:</p>
                    <div style="font-size: 36px; font-weight: 700; letter-spacing: 8px; color: #1e293b; padding: 16px 0;">
                      %s
                    </div>
                    <p style="color: #94a3b8; font-size: 13px; margin: 16px 0 0;">This code expires in <strong>10 minutes</strong>.</p>
                  </div>
                  <p style="color: #94a3b8; font-size: 12px; text-align: center; margin-top: 24px;">
                    If you didn't request this, you can safely ignore this email.
                  </p>
                </div>
                """.formatted(otp);
    }
}
