package ru.mtuci.coursemanagement.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mtuci.coursemanagement.config.JwtProperties;
import ru.mtuci.coursemanagement.model.AppUser;
import ru.mtuci.coursemanagement.model.Role;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties props;

    public String createAccessToken(AppUser user) {
        Instant now = Instant.now();
        List<String> roles = user.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList();
        return Jwts.builder()
                .issuer(props.issuer())
                .subject(user.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.accessTtl())))
                .claim("roles", roles)
                .signWith(props.signingKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(props.signingKey())
                .requireIssuer(props.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @SuppressWarnings("unchecked")
    public List<String> rolesFromClaims(Claims claims) {
        Object r = claims.get("roles");
        if (r instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    /** Минимальный пользователь только из JWT (повторное чтение из БД в фильтре). */
    public AppUser syntheticUserFromClaims(Claims claims) {
        List<String> roleNames = rolesFromClaims(claims).stream()
                .map(s -> s.replace("ROLE_", ""))
                .toList();
        Role role = roleNames.stream()
                .map(Role::valueOf)
                .findFirst()
                .orElse(Role.STUDENT);
        AppUser synthetic = new AppUser();
        synthetic.setUsername(claims.getSubject());
        synthetic.setRole(role);
        synthetic.setEncodedPassword("");
        return synthetic;
    }
}
