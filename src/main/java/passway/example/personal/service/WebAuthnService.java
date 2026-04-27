package passway.example.personal.service;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.RegistrationRequest;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import passway.example.personal.dto.WebAuthnResponse;
import passway.example.personal.model.MfaDevice;
import passway.example.personal.repository.MfaDeviceRepository;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebAuthnService {

        private final MfaDeviceRepository mfaDeviceRepository;

        @Value("${app.webauthn.rp-id}")
        private String rpId;

        @Value("${app.webauthn.rp-name}")
        private String rpName;

        @Value("${app.webauthn.origin}")
        private String origin;

        private final WebAuthnManager webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();
        private final SecureRandom secureRandom = new SecureRandom();

        // In-memory challenge store (production: use Redis or DB)
        private final ConcurrentHashMap<String, Challenge> challengeStore = new ConcurrentHashMap<>();

        public WebAuthnResponse generateRegistrationOptions(String userId, String username) {
                byte[] challengeBytes = new byte[32];
                secureRandom.nextBytes(challengeBytes);
                Challenge challenge = new DefaultChallenge(challengeBytes);

                challengeStore.put(userId, challenge);

                List<MfaDevice> existingDevices = mfaDeviceRepository.findByUserId(userId);

                Map<String, Object> options = new HashMap<>();
                options.put("challenge", Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes));
                options.put("rp", Map.of("name", rpName, "id", rpId));
                options.put("user", Map.of(
                                "id", Base64.getUrlEncoder().withoutPadding().encodeToString(userId.getBytes()),
                                "name", username,
                                "displayName", username));
                options.put("pubKeyCredParams", List.of(
                                Map.of("type", "public-key", "alg", -7), // ES256
                                Map.of("type", "public-key", "alg", -257) // RS256
                ));
                options.put("timeout", 60000);
                options.put("attestation", "none");
                options.put("authenticatorSelection", Map.of(
                                "authenticatorAttachment", "platform",
                                "requireResidentKey", false,
                                "userVerification", "preferred"));
                options.put("excludeCredentials", existingDevices.stream()
                                .map(d -> Map.of("type", "public-key", "id", d.getCredentialId()))
                                .toList());

                return WebAuthnResponse.builder()
                                .options(options)
                                .challenge(Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes))
                                .success(true)
                                .message("Registration options generated")
                                .build();
        }

        public WebAuthnResponse verifyRegistration(String userId, String deviceName,
                        byte[] attestationObject, byte[] clientDataJSON) {
                try {
                        Challenge challenge = challengeStore.remove(userId);
                        if (challenge == null) {
                                return WebAuthnResponse.builder()
                                                .success(false)
                                                .message("No registration challenge found. Please restart registration.")
                                                .build();
                        }

                        Origin rpOrigin = new Origin(origin);
                        ServerProperty serverProperty = new ServerProperty(rpOrigin, rpId, challenge, null);

                        RegistrationRequest registrationRequest = new RegistrationRequest(attestationObject,
                                        clientDataJSON);
                        RegistrationParameters registrationParameters = new RegistrationParameters(
                                        serverProperty, null, false, true);

                        RegistrationData registrationData = webAuthnManager.parse(registrationRequest);
                        webAuthnManager.validate(registrationData, registrationParameters);

                        byte[] credentialId = registrationData.getAttestationObject()
                                        .getAuthenticatorData().getAttestedCredentialData().getCredentialId();
                        byte[] publicKey = registrationData.getAttestationObject()
                                        .getAuthenticatorData().getAttestedCredentialData().getCOSEKey().getPublicKey()
                                        .getEncoded();

                        MfaDevice device = MfaDevice.builder()
                                        .userId(userId)
                                        .credentialId(Base64.getUrlEncoder().withoutPadding()
                                                        .encodeToString(credentialId))
                                        .publicKey(publicKey)
                                        .signCount(registrationData.getAttestationObject()
                                                        .getAuthenticatorData().getSignCount())
                                        .deviceName(deviceName != null ? deviceName : "Passkey Device")
                                        .build();

                        mfaDeviceRepository.save(device);

                        log.info("WebAuthn device registered for user {}: {}", userId, device.getDeviceName());

                        return WebAuthnResponse.builder()
                                        .success(true)
                                        .message("Passkey registered successfully")
                                        .build();

                } catch (Exception e) {
                        log.error("WebAuthn registration verification failed for user {}: {}", userId, e.getMessage());
                        return WebAuthnResponse.builder()
                                        .success(false)
                                        .message("Passkey registration failed: " + e.getMessage())
                                        .build();
                }
        }

        public WebAuthnResponse generateAuthenticationOptions(String userId) {
                List<MfaDevice> devices = mfaDeviceRepository.findByUserId(userId);

                if (devices.isEmpty()) {
                        return WebAuthnResponse.builder()
                                        .success(false)
                                        .message("No passkeys registered for this account")
                                        .build();
                }

                byte[] challengeBytes = new byte[32];
                secureRandom.nextBytes(challengeBytes);
                Challenge challenge = new DefaultChallenge(challengeBytes);
                challengeStore.put(userId, challenge);

                Map<String, Object> options = new HashMap<>();
                options.put("challenge", Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes));
                options.put("timeout", 60000);
                options.put("rpId", rpId);
                options.put("allowCredentials", devices.stream()
                                .map(d -> Map.of("type", "public-key", "id", d.getCredentialId()))
                                .toList());
                options.put("userVerification", "preferred");

                return WebAuthnResponse.builder()
                                .options(options)
                                .challenge(Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes))
                                .success(true)
                                .message("Authentication options generated")
                                .build();
        }

        public boolean verifyAuthentication(String userId, String credentialIdBase64) {
                try {
                        Challenge challenge = challengeStore.remove(userId);
                        if (challenge == null) {
                                log.warn("No authentication challenge found for user {}", userId);
                                return false;
                        }

                        List<MfaDevice> devices = mfaDeviceRepository.findByUserId(userId);
                        boolean deviceExists = devices.stream().anyMatch(d -> d.getCredentialId().equals(credentialIdBase64));

                        if (deviceExists) {
                                log.info("WebAuthn authentication successful for user {}", userId);
                                return true;
                        } else {
                                log.warn("Unknown credential ID {} for user {}", credentialIdBase64, userId);
                                return false;
                        }
                } catch (Exception e) {
                        log.error("WebAuthn authentication failed: {}", e.getMessage());
                        return false;
                }
        }

        public boolean hasRegisteredDevices(String userId) {
                return !mfaDeviceRepository.findByUserId(userId).isEmpty();
        }
}
