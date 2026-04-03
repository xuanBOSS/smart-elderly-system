package com.community.smartelderlybackend.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

public class JwtUtils {
    // 令牌有效期：7天
    private static final long EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L;
    // 自动生成安全的秘钥
    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // 生成令牌
    public static String generateToken(Long userId, Integer role) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("role", role) // 0老人 1家属 2医生 3社区 [cite: 104]
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .signWith(SECRET_KEY)
                .compact();
    }

    // 解析令牌
    public static Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}