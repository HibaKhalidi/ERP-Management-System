package com.erp.web;

import com.erp.domain.Permission;
import com.erp.repository.PermissionRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@Tag(name = "Rôles et permissions")
public class PermissionController {
    private final PermissionRepository repo;

    public PermissionController(PermissionRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Permission> list() {
        return repo.findAll();
    }
}
