package com.VER7U7.auth;

import com.VER7U7.utils.TraceUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final Logger LOGGER = LogManager.getLogger(JwtService.class);

    private final SecretKey key;
    private final long refreshExpirationTime;
    private final long accessExpirationTime;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.refresh_expiration}") long refreshExpirationTime,
            @Value("${jwt.access_expiration}") long accessExpirationTime) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.refreshExpirationTime = refreshExpirationTime;
        this.accessExpirationTime = accessExpirationTime;
    }

    public String generateRefreshToken(String playerId) {
        return Jwts.builder()
                .subject(playerId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationTime))
                .signWith(key)
                .compact();
    }

    public String generateAccessToken(String playerId) {
        return Jwts.builder()
                .subject(playerId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpirationTime))
                .signWith(key)
                .compact();
    }

    public String validateRefreshTokenAndGetPlayerId(String token) {
        try {
            if (token == null || token.isEmpty())
                return null;

            Claims payload = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return payload.getSubject();
        }catch(Exception e) {
            LOGGER.debug(e);
            return null;
        }
    }

    public String validateAccessToken(String token) {
        try {
            if (token == null || token.isEmpty())
                return null;

            Claims payload = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return payload.getSubject();
        }catch(Exception e) {
            LOGGER.debug(e);
            return null;
        }
    }
}
