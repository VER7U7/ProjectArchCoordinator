package com.VER7U7.repo;

import com.VER7U7.models.PlayerAccount;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface PlayerAccountRepository extends CrudRepository<PlayerAccount, Long> {
    Optional<PlayerAccount> findByPlayerNickName(String playerNickName);
}
