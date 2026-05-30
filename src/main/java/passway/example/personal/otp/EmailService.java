package passway.example.personal.otp;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import passway.example.personal.config.MailProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import jakarta.annotation.PostConstruct;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private String otpEmailTemplate;
    private String passwordResetEmailTemplate;

    @PostConstruct
    public void init() {
        try {
            try (InputStream inputStream = getClass().getResourceAsStream("/templates/otp-email.html")) {
                if (inputStream == null) {
                    throw new IOException("Template file 'otp-email.html' not found in classpath under /templates/");
                }
                otpEmailTemplate = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                log.info("Successfully loaded OTP email template from resources");
            }

            try (InputStream inputStream = getClass().getResourceAsStream("/templates/password-reset-email.html")) {
                if (inputStream == null) {
                    throw new IOException("Template file 'password-reset-email.html' not found in classpath under /templates/");
                }
                passwordResetEmailTemplate = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                log.info("Successfully loaded Password Reset email template from resources");
            }
        } catch (IOException e) {
            log.error("Failed to load email templates", e);
            throw new RuntimeException("Initialization of EmailService failed due to missing template", e);
        }
    }

    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailProperties.username());
            helper.setTo(toEmail);
            helper.setSubject("🔒 Your Passway MFA Verification Code");
            helper.setText(buildOtpEmailHtml(otpCode), true);

            mailSender.send(message);
            log.info("OTP email sent successfully to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailProperties.username());
            helper.setTo(toEmail);
            helper.setSubject("🔑 Reset Your Passway Password");
            helper.setText(buildPasswordResetEmailHtml(resetLink), true);

            mailSender.send(message);
            log.info("Password reset email sent successfully to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    private String buildOtpEmailHtml(String otpCode) {
        if (otpEmailTemplate == null) {
            throw new IllegalStateException("OTP Email template is not initialized");
        }
        return otpEmailTemplate.replace("{{otpCode}}", otpCode);
    }

    private String buildPasswordResetEmailHtml(String resetLink) {
        if (passwordResetEmailTemplate == null) {
            throw new IllegalStateException("Password reset email template is not initialized");
        }
        return passwordResetEmailTemplate.replace("{{resetLink}}", resetLink);
    }
}
