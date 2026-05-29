package passway.example.personal.mfa;

import lombok.Builder;

@Builder
public record TotpSetupResponse(
    String secret,
    String qrCodeUri,
    String message
) {}
