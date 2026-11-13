package com.erp.web;

import com.erp.common.BaseCrudController;
import com.erp.common.Refs;
import com.erp.domain.Role;
import com.erp.repository.PermissionRepository;
import com.erp.repository.RoleRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@Tag(name = "Rôles et permissions")
public class RoleController extends BaseCrudController<Role> {

    private final PermissionRepository permissions;

    public RoleController(RoleRepository repo, PermissionRepository permissions) {
        super(repo, "Rôle");
        this.permissions = permissions;
    }

    @Override
    protected List<String> searchFields() {
        return List.of("name", "description");
    }

    @Override
    protected void prepare(Role in, Role existing) {
        in.setPermissions(Refs.loadAll(permissions, in.getPermissions(), "Permission"));
    }
}
