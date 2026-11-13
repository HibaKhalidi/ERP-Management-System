package com.erp.web;

import com.erp.security.JwtService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification")
public class AuthController {

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record AuthResponse(String token, String tokenType, String username, List<String> roles, List<String> permissions) {}

    private final AuthenticationManager authManager;
    private final JwtService jwt;

    public AuthController(AuthenticationManager authManager, JwtService jwt) {
        this.authManager = authManager;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        UserDetails user = (UserDetails) auth.getPrincipal();
        return toResponse(jwt.generate(user), user);
    }

    @GetMapping("/me")
    public AuthResponse me(Authentication auth) {
        return toResponse(null, (UserDetails) auth.getPrincipal());
    }

    private AuthResponse toResponse(String token, UserDetails user) {
        List<String> all = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        List<String> roles = all.stream().filter(a -> a.startsWith("ROLE_")).map(a -> a.substring(5)).toList();
        List<String> perms = all.stream().filter(a -> !a.startsWith("ROLE_")).toList();
        return new AuthResponse(token, "Bearer", user.getUsername(), roles, perms);
    }
}
