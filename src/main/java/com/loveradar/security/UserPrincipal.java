package com.loveradar.security;

import com.loveradar.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Adapts the {@link User} entity to Spring Security's {@link UserDetails}
 * contract while exposing the underlying user id for use in services.
 *
 * ISSUE #3 FIX: Removed @Getter from the class.
 * @Getter on a class generates a getter for every field, including
 * getPassword() and getEmail(). But those same names are also declared as
 * @Override methods required by the UserDetails interface. This causes
 * javac to report "duplicate method" errors at compile time.
 * Solution: drop @Getter and expose the non-interface fields (id, email)
 * via explicit getters only where needed.
 */
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.id       = user.getId();
        this.email    = user.getEmail();
        this.password = user.getPassword();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    /** Exposed for UserService.getCurrentUser() to look up the DB record. */
    public UUID getId() {
        return id;
    }

    // ── UserDetails interface ────────────────────────────────────────────────

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired()     { return true; }

    @Override
    public boolean isAccountNonLocked()      { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled()               { return true; }
}
