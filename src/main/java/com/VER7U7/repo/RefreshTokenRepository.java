package com.VER7U7.repo;

import com.VER7U7.models.PlayerAccount;
import com.VER7U7.models.RefreshToken;
import org.springframework.data.repository.CrudRepository;

import java.util.List;


public interface RefreshTokenRepository extends CrudRepository<RefreshToken, Long> {
    List<RefreshToken> findByOwnerAccount(PlayerAccount account);
}
