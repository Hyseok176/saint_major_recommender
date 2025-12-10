package com.saintplus.common.security;

import com.saintplus.user.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;


public class UserPrincipal implements UserDetails {

    private final Long userId;

    public UserPrincipal(Long userId){
        this.userId = userId;
    }

    public static UserPrincipal from(User user){
        return new UserPrincipal(user.getId());
    }

    public Long getUserId(){
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();  // 권한 없음
    }

    @Override
    public String getPassword() {
        return null; // OAuth2 방식이므로 비밀번호 없음
    }

    @Override
    public String getUsername() {
        return String.valueOf(userId);
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }

}
