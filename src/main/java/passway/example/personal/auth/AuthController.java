package passway.example.personal.auth;
import passway.example.personal.mfa.WebAuthnResponse;
import passway.example.personal.exception.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import passway.example.personal.auth.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/mfa/verify")
    public AuthResponse verifyMfa(@Valid @RequestBody MfaVerifyRequest request) {
        return authService.verifyMfa(request);
    }

    @PostMapping("/mfa/otp/send")
    public ApiResponse sendLoginOtp(@Valid @RequestBody MfaSendRequest request) {
        authService.sendLoginOtp(request.mfaToken(), request.method());
        return ApiResponse.success("OTP sent");
    }

    @PostMapping("/mfa/webauthn/options")
    public WebAuthnResponse webAuthnLoginOptions(@Valid @RequestBody MfaSendRequest request) {
        return authService.getWebAuthnLoginOptions(request.mfaToken());
    }

    @PostMapping("/forgot-password")
    public ApiResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.success("If the email is registered, a password reset link has been sent");
    }

    @PostMapping("/reset-password")
    public ApiResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success("Password has been reset successfully");
    }

    @GetMapping("/health")
    public ApiResponse health() {
        return ApiResponse.success("Passway MFA Auth Service is running");
    }
}
