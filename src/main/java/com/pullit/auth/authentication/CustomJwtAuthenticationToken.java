package com.pullit.auth.authentication;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

public class CustomJwtAuthenticationToken extends AbstractAuthenticationToken {

    private final Jwt jwt;
    private final CustomUserDetails userDetails;

    public CustomJwtAuthenticationToken(Jwt jwt,
                                        CustomUserDetails userDetails,
                                        Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.jwt = jwt;
        this.userDetails = userDetails;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return jwt.getTokenValue();
    }

    @Override
    public Object getPrincipal() {
        return userDetails;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public String getTokenValue() {
        return jwt.getTokenValue();
    }
}
