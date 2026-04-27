package passway.example.personal.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import passway.example.personal.dto.ApiResponse;
import passway.example.personal.dto.AuthResponse;
import passway.example.personal.dto.LoginRequest;
import passway.example.personal.dto.MfaSendRequest;
import passway.example.personal.dto.MfaVerifyRequest;
import passway.example.personal.dto.RegisterRequest;
import passway.example.personal.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<AuthResponse> verifyMfa(@Valid @RequestBody MfaVerifyRequest request) {
        AuthResponse response = authService.verifyMfa(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mfa/otp/send")
    public ResponseEntity<ApiResponse> sendLoginOtp(@Valid @RequestBody MfaSendRequest request) {
        authService.sendLoginOtp(request.getMfaToken(), request.getMethod());
        return ResponseEntity.ok(ApiResponse.success("OTP sent"));
    }

    @PostMapping("/mfa/webauthn/options")
    public ResponseEntity<passway.example.personal.dto.WebAuthnResponse> webAuthnLoginOptions(@Valid @RequestBody MfaSendRequest request) {
        passway.example.personal.dto.WebAuthnResponse response = authService.getWebAuthnLoginOptions(request.getMfaToken());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse> health() {
        return ResponseEntity.ok(ApiResponse.success("Passway MFA Auth Service is running"));
    }
}
