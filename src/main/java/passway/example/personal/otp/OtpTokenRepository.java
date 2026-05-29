package passway.example.personal.otp;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import passway.example.personal.mfa.MfaMethod;
import passway.example.personal.otp.OtpToken;

import java.util.Optional;

@Repository
public interface OtpTokenRepository extends MongoRepository<OtpToken, String> {

    Optional<OtpToken> findFirstByUserIdAndTypeAndUsedFalseOrderByExpiresAtDesc(
            String userId, MfaMethod type);
}
