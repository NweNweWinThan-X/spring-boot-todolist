package com.todolist.shared.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Supplies the clock used by date-dependent views, so tests can pin "today".
 */
@Configuration
public class ClockConfig {

  /**
   * @return system clock in the JVM's default zone
   */
  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }
}
