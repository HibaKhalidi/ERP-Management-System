package com.erp.repository;

import com.erp.common.BaseRepository;
import com.erp.domain.AppUser;
import java.util.Optional;

public interface AppUserRepository extends BaseRepository<AppUser> {
    Optional<AppUser> findByUsername(String username);
    boolean existsByUsername(String username);
}
