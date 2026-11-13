package com.erp.web;

import com.erp.common.BaseCrudController;
import com.erp.common.BusinessException;
import com.erp.common.Refs;
import com.erp.domain.AppUser;
import com.erp.repository.AppUserRepository;
import com.erp.repository.RoleRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Corps : {"username","email","password","enabled","roles":[{"id":1}]}. Mot de passe vide en PUT = inchangé. */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Utilisateurs")
public class UserController extends BaseCrudController<AppUser> {

    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    public UserController(AppUserRepository repo, RoleRepository roles, PasswordEncoder encoder) {
        super(repo, "Utilisateur");
        this.roles = roles;
        this.encoder = encoder;
    }

    @Override
    protected List<String> searchFields() {
        return List.of("username", "email");
    }

    @Override
    protected void prepare(AppUser in, AppUser existing) {
        in.setRoles(Refs.loadAll(roles, in.getRoles(), "Rôle"));
        String raw = in.getPassword();
        if (raw == null || raw.isBlank()) {
            if (existing == null) {
                throw new BusinessException("Mot de passe obligatoire");
            }
            in.setPassword(existing.getPassword());
        } else {
            if (raw.length() < 6) {
                throw new BusinessException("Le mot de passe doit contenir au moins 6 caractères");
            }
            in.setPassword(encoder.encode(raw));
        }
    }
}
