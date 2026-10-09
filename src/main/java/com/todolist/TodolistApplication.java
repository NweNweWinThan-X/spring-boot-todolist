package com.todolist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point for the modular monolith.
 *
 * <p>Business modules live under {@code modules/}, cross-cutting concerns under {@code shared/}.
 */
@SpringBootApplication
public class TodolistApplication {

  /**
   * @param args command line arguments passed to the Spring context
   */
  public static void main(String[] args) {
    SpringApplication.run(TodolistApplication.class, args);
  }
}
