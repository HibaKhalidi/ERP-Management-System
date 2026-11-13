package com.erp.repository;

import com.erp.common.BaseRepository;
import com.erp.domain.Role;
import java.util.Optional;

public interface RoleRepository extends BaseRepository<Role> {
    Optional<Role> findByName(String name);
}
