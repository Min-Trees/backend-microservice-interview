package com.auth.service.service;

import com.auth.service.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.*;

@Service
public class JwtService {

    @Value("${jwt.private-key}")
    private String privateKeyPem;

    @Value("${jwt.public-key}")
    private String publicKeyPem;

    @Value("${jwt.key-id}")
    private String keyId;

    @Value("${jwt.access-minutes}")
    private long accessMinutes;

    @Value("${jwt.refresh-days}")
    private long refreshDays;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    void initKeys() {
        try {
            this.privateKey = loadPrivateKey(privateKeyPem);
            this.publicKey = loadPublicKey(publicKeyPem);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA keys", e);
        }
    }

    public long getAccessTokenTtl() {
        return accessMinutes * 60;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessMinutes * 60);
        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .setHeaderParam("kid", keyId)
                .compact();
    }

    public TokenInfo generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(refreshDays * 24 * 60 * 60);
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .setId(jti)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .setHeaderParam("kid", keyId)
                .compact();
        return new TokenInfo(token, jti, exp);
    }

    public TokenInfo parseToken(String token) {
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token);
        String jti = jws.getBody().getId();
        Instant exp = jws.getBody().getExpiration().toInstant();
        return new TokenInfo(token, jti, exp);
    }

    public Map<String, Object> getJwks() {
        RSAPublicKey rsa = (RSAPublicKey) this.publicKey;
        String n = Base64.getUrlEncoder().withoutPadding().encodeToString(stripLeadingZero(rsa.getModulus().toByteArray()));
        String e = Base64.getUrlEncoder().withoutPadding().encodeToString(stripLeadingZero(rsa.getPublicExponent().toByteArray()));
        Map<String, Object> jwk = new HashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("kid", keyId);
        jwk.put("alg", "RS256");
        jwk.put("use", "sig");
        jwk.put("n", n);
        jwk.put("e", e);
        return Map.of("keys", List.of(jwk));
    }

    private PrivateKey loadPrivateKey(String pem) throws Exception {
        String content = pem.replaceAll("-----END PRIVATE KEY-----", "")
                .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll("\n", "");
        byte[] decoded = Base64.getDecoder().decode(content);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    private PublicKey loadPublicKey(String pem) throws Exception {
        String content = pem.replaceAll("-----END PUBLIC KEY-----", "")
                .replaceAll("-----BEGIN PUBLIC KEY-----", "")
                .replaceAll("\n", "");
        byte[] decoded = Base64.getDecoder().decode(content);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    private byte[] stripLeadingZero(byte[] bytes) {
        if (bytes[0] == 0) {
            return Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return bytes;
    }

    public record TokenInfo(String token, String jti, Instant expiresAt) {}
}
