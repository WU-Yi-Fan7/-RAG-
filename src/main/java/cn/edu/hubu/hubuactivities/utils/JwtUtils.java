package cn.edu.hubu.hubuactivities.utils;

import cn.edu.hubu.hubuactivities.entity.Student;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refreshExpiration}")
    private Long refreshExpiration;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(Student student) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", student.getId());
        claims.put("stu_id", student.getStu_id());  // 注意字段名
        claims.put("real_name", student.getReal_name());
        claims.put("role", student.getRole());

        return createToken(claims, expiration);
    }

    public String generateRefreshToken(Student student) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", student.getId());
        claims.put("stu_id", student.getStu_id());
        claims.put("type", "refresh");

        return createToken(claims, refreshExpiration);
    }

    private String createToken(Map<String, Object> claims, Long expireTime) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expireTime);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expireDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.error("Token已过期: {}", e.getMessage());
            throw new RuntimeException("Token已过期");
        } catch (JwtException e) {
            log.error("Token无效: {}", e.getMessage());
            throw new RuntimeException("Token无效");
        }
    }

    public String getStuIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("stu_id", String.class);
    }

    public Long getIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("id", Long.class);
    }

    public Boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}