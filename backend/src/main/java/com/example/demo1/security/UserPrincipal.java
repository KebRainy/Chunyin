package com.example.demo1.security;

import com.example.demo1.common.enums.UserRole;
import com.example.demo1.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {
    private final Long id;
    private final String username;
    private final String password;
    private final UserRole role;
    private final boolean active;

    private final Collection<? extends GrantedAuthority> authorities;

    private UserPrincipal(Long id, String username, String password, UserRole role, boolean active) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.active = active;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public static UserPrincipal fromUser(User user) {
        return new UserPrincipal(
            user.getId(),
            user.getUsername(),
            user.getPassword(),
            user.getRole(),
            Boolean.TRUE.equals(user.getIsActive())
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}

