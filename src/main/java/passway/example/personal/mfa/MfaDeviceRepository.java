package passway.example.personal.mfa;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import passway.example.personal.mfa.MfaDevice;

import java.util.List;

@Repository
public interface MfaDeviceRepository extends MongoRepository<MfaDevice, String> {

    List<MfaDevice> findByUserId(String userId);

    void deleteByUserId(String userId);
}
