package passway.example.personal.mfa;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import passway.example.personal.exception.ApiResponse;
import passway.example.personal.otp.OtpRequest;
import passway.example.personal.mfa.TotpSetupResponse;
import passway.example.personal.mfa.WebAuthnResponse;
import passway.example.personal.mfa.MfaMethod;
import passway.example.personal.mfa.MfaService;

import java.util.Map;

@RestController
@RequestMapping("/api/mfa")
@RequiredArgsConstructor
public class MfaController {

    private final MfaService mfaService;

    // ═══════════════════════════════ TOTP ═══════════════════════════════

    @PostMapping("/totp/setup")
    public TotpSetupResponse setupTotp(@AuthenticationPrincipal UserDetails userDetails) {
        return mfaService.initiateTotpSetup(userDetails.getUsername());
    }

    @PostMapping("/totp/verify-setup")
    public ApiResponse verifyTotpSetup(@AuthenticationPrincipal UserDetails userDetails, @RequestParam String code) {
        mfaService.verifyAndEnableTotp(userDetails.getUsername(), code);
        return ApiResponse.success("TOTP MFA enabled successfully. Use Google Authenticator for future logins.");
    }

    // ═══════════════════════════ EMAIL / SMS OTP ═══════════════════════

    @PostMapping("/otp/send")
    public ApiResponse sendOtp(@AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody OtpRequest request) {
        mfaService.sendOtp(userDetails.getUsername(), request.method());
        return ApiResponse.success("OTP sent via " + (request.method() == MfaMethod.EMAIL_OTP ? "email" : "SMS"));
    }

    @PostMapping("/otp/enable")
    public ApiResponse enableOtp(@AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody OtpRequest request, @RequestParam String code) {
        mfaService.verifyAndEnableOtp(userDetails.getUsername(), request.method(), code);
        return ApiResponse.success((request.method() == MfaMethod.EMAIL_OTP ? "Email" : "SMS") + " OTP MFA enabled successfully");
    }

    // ═══════════════════════════ WEBAUTHN ═══════════════════════════════

    @PostMapping("/webauthn/register/options")
    public WebAuthnResponse webAuthnRegisterOptions(@AuthenticationPrincipal UserDetails userDetails) {
        return mfaService.getWebAuthnRegisterOptions(userDetails.getUsername());
    }

    @PostMapping("/webauthn/register/verify")
    public WebAuthnResponse webAuthnRegisterVerify(@AuthenticationPrincipal UserDetails userDetails, @RequestBody Map<String, String> body) {
        return mfaService.verifyAndEnableWebAuthn(userDetails.getUsername(), body);
    }

    @PostMapping("/webauthn/authenticate/options")
    public WebAuthnResponse webAuthnAuthOptions(@AuthenticationPrincipal UserDetails userDetails) {
        return mfaService.getWebAuthnAuthOptions(userDetails.getUsername());
    }

    // ═══════════════════════════ MANAGEMENT ═══════════════════════════

    @GetMapping("/methods")
    public ApiResponse getMfaMethods(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> status = mfaService.getMfaMethodsStatus(userDetails.getUsername());
        return ApiResponse.success("MFA methods retrieved", status);
    }

    @DeleteMapping("/{method}")
    public ApiResponse disableMfaMethod(@AuthenticationPrincipal UserDetails userDetails, @PathVariable MfaMethod method) {
        mfaService.disableMfaMethod(userDetails.getUsername(), method);
        return ApiResponse.success("MFA method " + method + " disabled");
    }
}
