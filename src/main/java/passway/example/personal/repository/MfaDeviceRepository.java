package passway.example.personal.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import passway.example.personal.model.MfaDevice;

import java.util.List;
import java.util.Optional;

@Repository
public interface MfaDeviceRepository extends MongoRepository<MfaDevice, String> {

    List<MfaDevice> findByUserId(String userId);

    Optional<MfaDevice> findByCredentialId(String credentialId);

    void deleteByUserId(String userId);
}
