package passway.example.personal.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import passway.example.personal.model.MfaMethod;
import passway.example.personal.model.OtpToken;

import java.util.Optional;

@Repository
public interface OtpTokenRepository extends MongoRepository<OtpToken, String> {

    Optional<OtpToken> findFirstByUserIdAndTypeAndUsedFalseOrderByExpiresAtDesc(
            String userId, MfaMethod type);

    void deleteByUserId(String userId);
}
