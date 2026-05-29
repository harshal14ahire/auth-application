package passway.example.personal.otp;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import passway.example.personal.config.AppProperties;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final AppProperties appProperties;

    @PostConstruct
    public void init() {
        String accountSid = appProperties.twilio().accountSid();
        String authToken = appProperties.twilio().authToken();
        if (accountSid != null && !accountSid.startsWith("your-")) {
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

        String accountSid = appProperties.twilio().accountSid();
        if (accountSid == null || accountSid.startsWith("your-")) {
            log.warn("TWILIO NOT CONFIGURED — SMS OTP for {}: {}", toPhoneNumber, otpCode);
            return;
        }

        try {
            Message message = Message.creator(
                    new PhoneNumber(toPhoneNumber),
                    new PhoneNumber(appProperties.twilio().phoneNumber()),
                    messageBody).create();

            log.info("SMS OTP sent successfully. SID: {}, To: {}", message.getSid(), toPhoneNumber);
        } catch (Exception e) {
            log.error("Failed to send SMS OTP to {}: {}", toPhoneNumber, e.getMessage());
            throw new RuntimeException("Failed to send SMS OTP", e);
        }
    }
}
