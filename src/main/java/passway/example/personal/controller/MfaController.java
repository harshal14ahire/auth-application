package passway.example.personal.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import passway.example.personal.dto.ApiResponse;
import passway.example.personal.dto.OtpRequest;
import passway.example.personal.dto.TotpSetupResponse;
import passway.example.personal.dto.WebAuthnResponse;
import passway.example.personal.model.MfaMethod;
import passway.example.personal.model.User;
import passway.example.personal.repository.MfaDeviceRepository;
import passway.example.personal.repository.UserRepository;
import passway.example.personal.service.OtpService;
import passway.example.personal.service.TotpService;
import passway.example.personal.service.WebAuthnService;

import java.util.Base64;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/mfa")
@RequiredArgsConstructor
public class MfaController {

    private final TotpService totpService;
    private final OtpService otpService;
    private final WebAuthnService webAuthnService;
    private final UserRepository userRepository;
    private final MfaDeviceRepository mfaDeviceRepository;

    // ═══════════════════════════════ TOTP ═══════════════════════════════

    @PostMapping("/totp/setup")
    public ResponseEntity<TotpSetupResponse> setupTotp(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails.getUsername());
        String secret = totpService.generateSecret();

        // Save secret temporarily — will be confirmed when user verifies
        user.setTotpSecret(secret);
        userRepository.save(user);

        TotpSetupResponse response = totpService.generateSetupInfo(user.getUsername(), secret);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/totp/verify-setup")
    public ResponseEntity<ApiResponse> verifyTotpSetup(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String code) {

        User user = getUser(userDetails.getUsername());

        if (user.getTotpSecret() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TOTP setup not initiated. Call /totp/setup first."));
        }

        boolean valid = totpService.verifyCode(user.getTotpSecret(), code);
        if (!valid) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid TOTP code. Please try again."));
        }

        // Enable TOTP MFA
        if (!user.getMfaMethods().contains(MfaMethod.TOTP)) {
            user.getMfaMethods().add(MfaMethod.TOTP);
        }
        user.setMfaEnabled(true);
        userRepository.save(user);

        log.info("TOTP MFA enabled for user: {}", user.getUsername());
        return ResponseEntity
                .ok(ApiResponse.success("TOTP MFA enabled successfully. Use Google Authenticator for future logins."));
    }

    // ═══════════════════════════ EMAIL / SMS OTP ═══════════════════════

    @PostMapping("/otp/send")
    public ResponseEntity<ApiResponse> sendOtp(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OtpRequest request) {

        User user = getUser(userDetails.getUsername());

        otpService.generateAndSendOtp(user.getId(), request.getMethod());

        String channel = request.getMethod() == MfaMethod.EMAIL_OTP ? "email" : "SMS";
        return ResponseEntity.ok(ApiResponse.success("OTP sent via " + channel));
    }

    @PostMapping("/otp/enable")
    public ResponseEntity<ApiResponse> enableOtp(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OtpRequest request,
            @RequestParam String code) {

        User user = getUser(userDetails.getUsername());
        MfaMethod method = request.getMethod();

        boolean valid = otpService.verifyOtp(user.getId(), code, method);
        if (!valid) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid OTP code"));
        }

        if (!user.getMfaMethods().contains(method)) {
            user.getMfaMethods().add(method);
        }
        user.setMfaEnabled(true);
        userRepository.save(user);

        String channel = method == MfaMethod.EMAIL_OTP ? "Email" : "SMS";
        log.info("{} OTP MFA enabled for user: {}", channel, user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(channel + " OTP MFA enabled successfully"));
    }

    // ═══════════════════════════ WEBAUTHN ═══════════════════════════════

    @PostMapping("/webauthn/register/options")
    public ResponseEntity<WebAuthnResponse> webAuthnRegisterOptions(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails.getUsername());
        WebAuthnResponse response = webAuthnService.generateRegistrationOptions(user.getId(), user.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webauthn/register/verify")
    public ResponseEntity<WebAuthnResponse> webAuthnRegisterVerify(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> body) {

        User user = getUser(userDetails.getUsername());

        String deviceName = (String) body.getOrDefault("deviceName", "My Passkey");
        String attestationObjectB64 = (String) body.get("attestationObject");
        String clientDataJSONB64 = (String) body.get("clientDataJSON");

        byte[] attestationObject = Base64.getUrlDecoder().decode(attestationObjectB64);
        byte[] clientDataJSON = Base64.getUrlDecoder().decode(clientDataJSONB64);

        WebAuthnResponse response = webAuthnService.verifyRegistration(
                user.getId(), deviceName, attestationObject, clientDataJSON);

        if (response.isSuccess()) {
            if (!user.getMfaMethods().contains(MfaMethod.WEBAUTHN)) {
                user.getMfaMethods().add(MfaMethod.WEBAUTHN);
            }
            user.setMfaEnabled(true);
            userRepository.save(user);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/webauthn/authenticate/options")
    public ResponseEntity<WebAuthnResponse> webAuthnAuthOptions(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails.getUsername());
        WebAuthnResponse response = webAuthnService.generateAuthenticationOptions(user.getId());
        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════ MANAGEMENT ═══════════════════════════

    @GetMapping("/methods")
    public ResponseEntity<ApiResponse> getMfaMethods(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("MFA methods retrieved", Map.of(
                "mfaEnabled", user.isMfaEnabled(),
                "methods", user.getMfaMethods())));
    }

    @DeleteMapping("/{method}")
    public ResponseEntity<ApiResponse> disableMfaMethod(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable MfaMethod method) {

        User user = getUser(userDetails.getUsername());

        user.getMfaMethods().remove(method);

        // Clean up method-specific data
        switch (method) {
            case TOTP -> user.setTotpSecret(null);
            case WEBAUTHN -> mfaDeviceRepository.deleteByUserId(user.getId());
            default -> {
                /* OTP tokens auto-expire via TTL */ }
        }

        if (user.getMfaMethods().isEmpty()) {
            user.setMfaEnabled(false);
        }

        userRepository.save(user);

        log.info("MFA method {} disabled for user: {}", method, user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("MFA method " + method + " disabled"));
    }

    // ═══════════════════════════════════════════════════════════════════════

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }
}
