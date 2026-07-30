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


    /**
     * Generates a refresh token and embeds the playerID into its subject.
     *
     * @param playerId the player ID from database in {@code String} format
     * @return the generated JWT access token with the configured expiration time
     * */
    public String generateRefreshToken(String playerId) {
        return Jwts.builder()
                .subject(playerId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationTime))
                .signWith(key)
                .compact();
    }

    /**
     * Generates an access token and embeds the playerID into its subject.
     *
     * @param playerId the player ID from database in {@code String} format
     * @return the generated JWT access token with the configured expiration time
     * */
    public String generateAccessToken(String playerId) {
        return Jwts.builder()
                .subject(playerId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpirationTime))
                .signWith(key)
                .compact();
    }


    /**
     * Validates the refresh token and extracts the player ID form it.
     *
     * @param token the JWT refresh token issued to client
     * @return the player ID if validation is successful; {@code null} if the refresh token is invalid,
     *         expired, or empty.
     * */
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

    /**
     * Validates the access token and extracts the player ID from it.
     *
     * @param token the JWT access token issued to client
     * @return the playerID if validation successful; {@code null} if the refresh token is invalid,
     *         expired, or empty
     * */
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
