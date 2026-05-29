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

    @GetMapping("/health")
    public ApiResponse health() {
        return ApiResponse.success("Passway MFA Auth Service is running");
    }
}
