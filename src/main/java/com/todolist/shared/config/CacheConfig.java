package com.todolist.shared.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-process caching backed by Caffeine.
 */
@Configuration
@EnableCaching
public class CacheConfig {

  private static final int MAX_ENTRIES = 1_000;
  private static final Duration EXPIRE_AFTER_WRITE = Duration.ofMinutes(10);

  /**
   * @return cache manager whose caches expire shortly after write
   */
  @Bean
  public CaffeineCacheManager cacheManager() {
    CaffeineCacheManager manager = new CaffeineCacheManager();
    manager.setCaffeine(Caffeine.newBuilder()
        .maximumSize(MAX_ENTRIES)
        .expireAfterWrite(EXPIRE_AFTER_WRITE));
    return manager;
  }
}
