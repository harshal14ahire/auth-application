package passway.example.personal.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsService {

    @Value("${app.twilio.account-sid}")
    private String accountSid;

    @Value("${app.twilio.auth-token}")
    private String authToken;

    @Value("${app.twilio.phone-number}")
    private String fromPhoneNumber;

    @PostConstruct
    public void init() {
        if (!accountSid.startsWith("your-")) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio SDK initialized successfully");
        } else {
            log.warn("Twilio credentials not configured. SMS OTP will be logged to console only.");
        }
    }

    public void sendOtpSms(String toPhoneNumber, String otpCode) {
        String messageBody = String.format(
                "🔐 Passway MFA: Your verification code is %s. It expires in 5 minutes. Do not share this code.",
                otpCode);

        if (accountSid.startsWith("your-")) {
            log.warn("TWILIO NOT CONFIGURED — SMS OTP for {}: {}", toPhoneNumber, otpCode);
            return;
        }

        try {
            Message message = Message.creator(
                    new PhoneNumber(toPhoneNumber),
                    new PhoneNumber(fromPhoneNumber),
                    messageBody).create();

            log.info("SMS OTP sent successfully. SID: {}, To: {}", message.getSid(), toPhoneNumber);
        } catch (Exception e) {
            log.error("Failed to send SMS OTP to {}: {}", toPhoneNumber, e.getMessage());
            throw new RuntimeException("Failed to send SMS OTP", e);
        }
    }
}
