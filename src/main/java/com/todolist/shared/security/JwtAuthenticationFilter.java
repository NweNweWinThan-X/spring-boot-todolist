package com.todolist.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Populates the security context from a {@code Authorization: Bearer <token>} header.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String HEADER_NAME = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenProvider tokenProvider;
  private final UserDetailsService userDetailsService;

  /**
   * @param tokenProvider verifies the bearer token
   * @param userDetailsService loads the account named by the token subject
   */
  public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
      UserDetailsService userDetailsService) {
    this.tokenProvider = tokenProvider;
    this.userDetailsService = userDetailsService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String token = resolveToken(request);
    if (token != null && tokenProvider.validateToken(token)) {
      authenticate(request, token);
    }
    filterChain.doFilter(request, response);
  }

  private void authenticate(HttpServletRequest request, String token) {
    try {
      UserDetails userDetails = userDetailsService.loadUserByUsername(
          tokenProvider.getUsernameFromToken(token));
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authentication);
    } catch (UsernameNotFoundException e) {
      // the account was removed or disabled after the token was issued; stay anonymous
      logger.debug("トークンの対象アカウントが見つかりません");
    }
  }

  private String resolveToken(HttpServletRequest request) {
    String header = request.getHeader(HEADER_NAME);
    if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
      return header.substring(BEARER_PREFIX.length());
    }
    return null;
  }
}
