package com.erp.security;

import com.erp.domain.AppUser;
import com.erp.domain.Permission;
import com.erp.domain.Role;
import com.erp.repository.AppUserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    public AppUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser u = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + username));
        return User.withUsername(u.getUsername())
                .password(u.getPassword())
                .disabled(!u.isEnabled())
                .authorities(authorities(u))
                .build();
    }

    public static Set<GrantedAuthority> authorities(AppUser u) {
        Set<GrantedAuthority> set = new LinkedHashSet<>();
        for (Role r : u.getRoles()) {
            set.add(new SimpleGrantedAuthority("ROLE_" + r.getName()));
            for (Permission p : r.getPermissions()) {
                set.add(new SimpleGrantedAuthority(p.getName()));
            }
        }
        return set;
    }
}
