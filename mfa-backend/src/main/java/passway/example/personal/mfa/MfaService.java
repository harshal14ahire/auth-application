package passway.example.personal.mfa;
import passway.example.personal.otp.OtpService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import passway.example.personal.mfa.TotpSetupResponse;
import passway.example.personal.mfa.WebAuthnResponse;
import passway.example.personal.mfa.MfaMethod;
import passway.example.personal.user.User;
import passway.example.personal.mfa.MfaDeviceRepository;
import passway.example.personal.user.UserRepository;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MfaService {

    private final TotpService totpService;
    private final OtpService otpService;
    private final WebAuthnService webAuthnService;
    private final UserRepository userRepository;
    private final MfaDeviceRepository mfaDeviceRepository;

    public TotpSetupResponse initiateTotpSetup(String username) {
        User user = getUser(username);
        String secret = totpService.generateSecret();

        user.setTotpSecret(secret);
        userRepository.save(user);

        return totpService.generateSetupInfo(username, secret);
    }

    public void verifyAndEnableTotp(String username, String code) {
        User user = getUser(username);

        if (user.getTotpSecret() == null) {
            throw new IllegalArgumentException("TOTP setup not initiated. Call /totp/setup first.");
        }

        boolean valid = totpService.verifyCode(user.getTotpSecret(), code);
        if (!valid) {
            throw new IllegalArgumentException("Invalid TOTP code. Please try again.");
        }

        enableMfaMethod(user, MfaMethod.TOTP);
    }

    public void sendOtp(String username, MfaMethod method) {
        User user = getUser(username);
        otpService.generateAndSendOtp(user.getId(), method);
    }

    public void verifyAndEnableOtp(String username, MfaMethod method, String code) {
        User user = getUser(username);

        boolean valid = otpService.verifyOtp(user.getId(), code, method);
        if (!valid) {
            throw new IllegalArgumentException("Invalid OTP code");
        }

        enableMfaMethod(user, method);
    }

    public WebAuthnResponse getWebAuthnRegisterOptions(String username) {
        User user = getUser(username);
        return webAuthnService.generateRegistrationOptions(user.getId(), user.getUsername());
    }

    public WebAuthnResponse verifyAndEnableWebAuthn(String username, Map<String, String> body) {
        User user = getUser(username);

        String deviceName = body.getOrDefault("deviceName", "My Passkey");
        String attestationObjectB64 = body.get("attestationObject");
        String clientDataJSONB64 = body.get("clientDataJSON");

        byte[] attestationObject = java.util.Base64.getUrlDecoder().decode(attestationObjectB64);
        byte[] clientDataJSON = java.util.Base64.getUrlDecoder().decode(clientDataJSONB64);

        WebAuthnResponse response = webAuthnService.verifyRegistration(
                user.getId(), deviceName, attestationObject, clientDataJSON);

        if (response.isSuccess()) {
            enableMfaMethod(user, MfaMethod.WEBAUTHN);
        }

        return response;
    }

    public WebAuthnResponse getWebAuthnAuthOptions(String username) {
        User user = getUser(username);
        return webAuthnService.generateAuthenticationOptions(user.getId());
    }

    public Map<String, Object> getMfaMethodsStatus(String username) {
        User user = getUser(username);
        return Map.of(
                "mfaEnabled", user.isMfaEnabled(),
                "methods", user.getMfaMethods()
        );
    }

    public void disableMfaMethod(String username, MfaMethod method) {
        User user = getUser(username);

        user.getMfaMethods().remove(method);

        // Clean up method-specific data
        switch (method) {
            case TOTP -> user.setTotpSecret(null);
            case WEBAUTHN -> mfaDeviceRepository.deleteByUserId(user.getId());
            default -> { /* OTP tokens auto-expire via TTL */ }
        }

        if (user.getMfaMethods().isEmpty()) {
            user.setMfaEnabled(false);
        }

        userRepository.save(user);
        log.info("MFA method {} disabled for user: {}", method, username);
    }

    private void enableMfaMethod(User user, MfaMethod method) {
        if (!user.getMfaMethods().contains(method)) {
            user.getMfaMethods().add(method);
        }
        user.setMfaEnabled(true);
        userRepository.save(user);
        log.info("{} MFA enabled for user: {}", method, user.getUsername());
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }
}
