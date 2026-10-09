package com.todolist.shared.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Issues and verifies HMAC signed access tokens.
 *
 * <p>The signing key comes from {@code JWT_SECRET}. When that is absent a random key is generated
 * for the lifetime of the process, which keeps local development working without committing a
 * secret; issued tokens then stop verifying after a restart.
 */
@Component
public class JwtTokenProvider {

  private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

  private static final int MIN_SECRET_BITS = 256;

  private final SecretKey signingKey;
  private final Duration expiration;

  /**
   * @param secret Base64 encoded signing key of at least 256 bits, blank to generate one
   * @param expirationMillis access token lifetime in milliseconds
   */
  public JwtTokenProvider(@Value("${app.jwt.secret:}") String secret,
      @Value("${app.jwt.expiration-ms}") long expirationMillis) {
    this.signingKey = buildSigningKey(secret);
    this.expiration = Duration.ofMillis(expirationMillis);
  }

  /**
   * @param username subject the token is issued for
   * @return signed compact access token
   */
  public String generateToken(String username) {
    Instant issuedAt = Instant.now();
    return Jwts.builder()
        .subject(username)
        .issuedAt(Date.from(issuedAt))
        .expiration(Date.from(issuedAt.plus(expiration)))
        .signWith(signingKey)
        .compact();
  }

  /**
   * @param token compact access token
   * @return subject carried by the token
   * @throws JwtException when the token is malformed, expired or not correctly signed
   */
  public String getUsernameFromToken(String token) {
    return Jwts.parser()
        .verifyWith(signingKey)
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getSubject();
  }

  /**
   * @param token compact access token
   * @return whether the token is well formed, unexpired and correctly signed
   */
  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      // expected for malformed, expired, unsupported or tampered tokens
      log.debug("JWTの検証に失敗: reason={}", e.getClass().getSimpleName());
      return false;
    }
  }

  /**
   * @return access token lifetime in seconds, for the client to schedule re-authentication
   */
  public long getExpiresInSeconds() {
    return expiration.toSeconds();
  }

  private static SecretKey buildSigningKey(String secret) {
    if (StringUtils.isBlank(secret)) {
      log.warn("JWT_SECRET が未設定のため一時的な署名鍵を生成しました。"
          + "再起動すると発行済みトークンは無効になります。");
      return Jwts.SIG.HS256.key().build();
    }
    byte[] keyBytes = Decoders.BASE64.decode(secret);
    if (keyBytes.length * Byte.SIZE < MIN_SECRET_BITS) {
      log.warn("JWT_SECRET が {} ビット未満です。256ビット以上の鍵を設定してください。",
          MIN_SECRET_BITS);
    }
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
