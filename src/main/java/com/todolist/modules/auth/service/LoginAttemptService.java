package com.todolist.modules.auth.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

/**
 * Brute force protection for the sign-in endpoint (security audit No.9).
 *
 * <p>Counters live in process. A clustered deployment needs a shared store instead.
 */
@Service
public class LoginAttemptService {

  private static final int MAX_ATTEMPTS = 5;
  private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

  private final Cache<String, AtomicInteger> attemptsByKey = Caffeine.newBuilder()
      .expireAfterWrite(BLOCK_DURATION)
      .maximumSize(10_000)
      .build();

  /**
   * @param key stable caller identity, normally the sign-in name
   */
  public void recordFailure(String key) {
    attemptsByKey.get(key, unused -> new AtomicInteger()).incrementAndGet();
  }

  /**
   * @param key stable caller identity, normally the sign-in name
   */
  public void reset(String key) {
    attemptsByKey.invalidate(key);
  }

  /**
   * @param key stable caller identity, normally the sign-in name
   * @return whether further attempts must be rejected
   */
  public boolean isBlocked(String key) {
    AtomicInteger attempts = attemptsByKey.getIfPresent(key);
    return attempts != null && attempts.get() >= MAX_ATTEMPTS;
  }
}
