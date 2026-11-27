
package LMS.LeaveManagement.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:3600000}") long expirationMs) {

        this.key = buildSigningKey(secret);
        this.expirationMs = expirationMs;
    }

    private static SecretKey buildSigningKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("app.jwt.secret must not be blank");
        }
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret.trim());
            if (keyBytes.length >= 32) {
                return Keys.hmacShaKeyFor(keyBytes);
            }
        } catch (RuntimeException ignored) {
            // Fall back to deterministic SHA-256 key derivation for plain/URL-safe secrets
        }
        try {
            byte[] hashed = MessageDigest.getInstance("SHA-256")
                    .digest(secret.trim().getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    public String generateToken(UserDetails user) {
        String primaryRole = user.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5))
                .findFirst()
                .orElse(RbacConstants.ROLE_USER);

        List<String> permissions = user.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(auth -> !auth.startsWith("ROLE_") && !RbacConstants.VALID_ROLES.contains(auth))
                .toList();
        if (permissions.isEmpty()) {
            permissions = RbacConstants.getPermissionsForRole(primaryRole);
        }

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", primaryRole)
                .claim("permissions", permissions)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(
            String token, UserDetails userDetails) {

        Claims claims = extractAllClaims(token);

        return claims.getSubject().equals(userDetails.getUsername())
                && claims.getExpiration().after(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
