package passway.example.personal.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
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

    private String buildOtpEmailHtml(String otpCode) {
        return """
                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 480px; margin: 0 auto;
                            background: linear-gradient(135deg, #0f0c29, #302b63, #24243e); border-radius: 16px;
                            padding: 40px; color: #ffffff;">
                    <h1 style="text-align: center; font-size: 24px; margin-bottom: 8px;">
                        🔐 Passway MFA
                    </h1>
                    <p style="text-align: center; color: #a0aec0; font-size: 14px; margin-bottom: 32px;">
                        Your one-time verification code
                    </p>
                    <div style="background: rgba(255,255,255,0.1); border-radius: 12px; padding: 24px;
                                text-align: center; margin-bottom: 24px; backdrop-filter: blur(10px);
                                border: 1px solid rgba(255,255,255,0.15);">
                        <span style="font-size: 36px; font-weight: 700; letter-spacing: 12px; color: #7c3aed;">
                            %s
                        </span>
                    </div>
                    <p style="text-align: center; color: #a0aec0; font-size: 13px;">
                        This code expires in <strong style="color: #f59e0b;">5 minutes</strong>.<br>
                        Do not share this code with anyone.
                    </p>
                </div>
                """.formatted(otpCode);
    }
}
