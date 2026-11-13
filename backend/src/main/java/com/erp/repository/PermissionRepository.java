package com.erp.repository;

import com.erp.common.BaseRepository;
import com.erp.domain.Permission;
import java.util.Optional;

public interface PermissionRepository extends BaseRepository<Permission> {
    Optional<Permission> findByName(String name);
}
