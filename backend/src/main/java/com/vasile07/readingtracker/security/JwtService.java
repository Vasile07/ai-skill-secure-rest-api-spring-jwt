package com.vasile07.readingtracker.security;

import com.vasile07.readingtracker.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final String issuer;
    private final long ttlMinutes;

    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.issuer}") String issuer,
                      @Value("${app.jwt.access-ttl-minutes:15}") long ttlMinutes) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.issuer = issuer;
        this.ttlMinutes = ttlMinutes;
    }

    public String access(User user) {
        return create(user, "access", ttlMinutes, true);
    }

    public String identity(User user) {
        return Jwts.builder()
                .subject(user.id().toString())
                .issuer(issuer)
                .claim("token_use", "id")
                .claim("name", user.name())
                .claim("email", user.email())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(ttlMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    private String create(User user, String use, long minutes, boolean roles) {
        var builder = Jwts.builder()
                .subject(user.id().toString())
                .issuer(issuer)
                .claim("token_use", use)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(minutes, ChronoUnit.MINUTES)));
        if (roles) {
            builder.claim("roles", user.roles());
        }
        return builder.signWith(key).compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
