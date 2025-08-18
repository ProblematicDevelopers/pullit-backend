package com.pullit.auth.converter;

import com.pullit.auth.authentication.CustomJwtAuthenticationToken;
import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.user.entity.UserRole;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Map<String, Object> claims = jwt.getClaims();

        CustomUserDetails userDetails = createUserDetails(claims);

        Collection<GrantedAuthority> authorities = extractAuthorities(claims);

        return new CustomJwtAuthenticationToken(jwt, userDetails, authorities);
    }

    private CustomUserDetails createUserDetails(Map<String, Object> claims) {
        Long userId = Long.parseLong(claims.get("sub").toString());
        String username = (String) claims.get("username");
        String email = (String) claims.get("email");
        String fullName = (String) claims.get("fullName");
        String roleString = (String) claims.get("role");

        UserRole role = UserRole.valueOf(roleString);

        return CustomUserDetails.builder()
                .userId(userId)
                .username(username)
                .email(email)
                .fullName(fullName)
                .role(role)
                .password(null)
                .build();
    }
    private Collection<GrantedAuthority> extractAuthorities(Map<String, Object> claims) {
        String roleString = (String) claims.get("role");
        UserRole role = UserRole.valueOf(roleString);

        // 단일 권한을 리스트로 반환
        return List.of(new SimpleGrantedAuthority(role.getAuthority()));
    }

}
