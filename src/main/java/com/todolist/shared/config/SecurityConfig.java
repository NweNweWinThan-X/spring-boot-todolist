package com.todolist.shared.config;

import com.todolist.shared.exception.AppErrorCode;
import com.todolist.shared.security.JwtAuthenticationFilter;
import com.todolist.shared.security.JwtTokenProvider;
import com.todolist.shared.web.ApiErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

/**
 * Security baseline.
 *
 * <p>Three chains are declared so that the relaxations each surface needs stay scoped to it: the
 * REST API is stateless and bearer authenticated, the H2 console needs same-origin framing, and
 * everything else is denied.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private static final String API_PATHS = "/api/**";
  private static final String AUTH_PATHS = "/api/v1/auth/login";
  private static final String REGISTER_PATH = "/api/v1/auth/register";
  private static final String H2_CONSOLE_PATHS = "/h2-console/**";
  private static final String HEALTH_PATH = "/actuator/health";
  private static final int BCRYPT_STRENGTH = 12;

  private static final String CSP_DIRECTIVES =
      "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; "
          + "frame-ancestors 'none'";

  private final ObjectMapper objectMapper;

  /**
   * @param objectMapper serialises error bodies written directly by the security filters
   */
  public SecurityConfig(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * @param http security builder supplied by Spring Security
   * @param tokenProvider verifies bearer tokens
   * @param userDetailsService loads the account named by a token
   * @return stateless chain for the REST API
   * @throws Exception when the chain cannot be built
   */
  @Bean
  @Order(1)
  public SecurityFilterChain apiFilterChain(HttpSecurity http, JwtTokenProvider tokenProvider,
      UserDetailsService userDetailsService) throws Exception {
    http
        .securityMatcher(API_PATHS)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        // Safe to disable: this chain is stateless and authenticates by bearer token only, so
        // there is no ambient cookie credential for a cross-site request to ride on.
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(requests -> requests
            .requestMatchers(AUTH_PATHS, REGISTER_PATH).permitAll()
            .anyRequest().authenticated()
        )
        .exceptionHandling(handling -> handling
            .authenticationEntryPoint((request, response, exception) ->
                writeError(response, AppErrorCode.UNAUTHENTICATED, request.getRequestURI()))
            .accessDeniedHandler((request, response, exception) ->
                writeError(response, AppErrorCode.ACCESS_DENIED, request.getRequestURI()))
        )
        .headers(headers -> headers
            .frameOptions(frame -> frame.deny())
            .contentSecurityPolicy(csp -> csp.policyDirectives(CSP_DIRECTIVES))
            .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.SAME_ORIGIN))
        )
        .addFilterBefore(new JwtAuthenticationFilter(tokenProvider, userDetailsService),
            UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  /**
   * Only registered when the H2 console is switched on, which the deployed profiles never do.
   *
   * @param http security builder supplied by Spring Security
   * @return chain for the H2 console
   * @throws Exception when the chain cannot be built
   */
  @Bean
  @Order(2)
  @ConditionalOnProperty(name = "spring.h2.console.enabled", havingValue = "true")
  public SecurityFilterChain h2ConsoleFilterChain(HttpSecurity http) throws Exception {
    http
        .securityMatcher(H2_CONSOLE_PATHS)
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
        // The console renders itself inside frames, so framing is relaxed for this path only.
        .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
    return http.build();
  }

  /**
   * Everything outside the API. The UI is a separate single page app, so nothing here is served
   * to a browser: the chain exists to deny by default and answer in JSON rather than redirect.
   *
   * @param http security builder supplied by Spring Security
   * @return fallback chain
   * @throws Exception when the chain cannot be built
   */
  @Bean
  @Order(3)
  public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(requests -> requests
            .requestMatchers(HEALTH_PATH).permitAll()
            .anyRequest().denyAll()
        )
        .exceptionHandling(handling -> handling
            .authenticationEntryPoint((request, response, exception) ->
                writeError(response, AppErrorCode.UNAUTHENTICATED, request.getRequestURI()))
            .accessDeniedHandler((request, response, exception) ->
                writeError(response, AppErrorCode.ACCESS_DENIED, request.getRequestURI()))
        )
        .headers(headers -> headers
            .frameOptions(frame -> frame.deny())
            .contentSecurityPolicy(csp -> csp.policyDirectives(CSP_DIRECTIVES))
            .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.SAME_ORIGIN))
        );
    return http.build();
  }

  /**
   * @return BCrypt encoder at the strength required by the security audit checklist
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
  }

  /**
   * @param configuration authentication configuration assembled by Spring Security
   * @return manager used by the sign-in endpoint to verify credentials
   * @throws Exception when the manager cannot be resolved
   */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
      throws Exception {
    return configuration.getAuthenticationManager();
  }

  /**
   * @return CORS policy allowing the Vite and CRA dev servers to call the API
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
    configuration.setAllowedMethods(
        List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration(API_PATHS, configuration);
    return source;
  }

  private void writeError(HttpServletResponse response, AppErrorCode errorCode, String path)
      throws IOException {
    HttpStatus status = errorCode.getStatus();
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    ApiErrorResponse body = new ApiErrorResponse(
        Instant.now(),
        status.value(),
        errorCode.getCode(),
        errorCode.getDefaultMessage(),
        path,
        Map.of()
    );
    response.getWriter().write(objectMapper.writeValueAsString(body));
  }
}
