package com.todolist.modules.task.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todolist.modules.task.domain.EisenhowerQuadrant;
import com.todolist.modules.task.domain.TaskPriority;
import com.todolist.modules.task.domain.TaskStatus;
import com.todolist.modules.task.dto.TaskResponseDto;
import com.todolist.modules.task.service.TaskService;
import com.todolist.shared.config.SecurityConfig;
import com.todolist.shared.exception.AppErrorCode;
import com.todolist.shared.exception.AppException;
import com.todolist.shared.security.JwtTokenProvider;
import com.todolist.shared.web.ApiExceptionHandler;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Locks the REST contract: auth gate, validation shape and status codes.
 */
@WebMvcTest(TaskController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
@ImportAutoConfiguration({
    SecurityAutoConfiguration.class,
    ServletWebSecurityAutoConfiguration.class,
    SecurityFilterAutoConfiguration.class,
    UserDetailsServiceAutoConfiguration.class
})
class TaskControllerTest {

  private static final String TASKS_PATH = "/api/v1/tasks";
  private static final String BEARER = "Bearer test-token";
  private static final String USERNAME = "alice";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TaskService taskService;

  @MockitoBean
  private JwtTokenProvider jwtTokenProvider;

  @MockitoBean
  private UserDetailsService userDetailsService;

  /**
   * The API chain is stateless, so {@code @WithMockUser} is discarded by
   * {@code SecurityContextHolderFilter}. Authenticating through the real filter also covers it.
   */
  private void givenAuthenticated() {
    when(jwtTokenProvider.validateToken("test-token")).thenReturn(true);
    when(jwtTokenProvider.getUsernameFromToken("test-token")).thenReturn(USERNAME);
    when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(User.withUsername(USERNAME)
        .password("n/a")
        .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
        .build());
  }

  private static TaskResponseDto sampleTask() {
    return new TaskResponseDto(1L, "請求書を作成する", "10月分", TaskStatus.PENDING,
        TaskPriority.HIGH, LocalDate.of(2026, 10, 15), true, true, EisenhowerQuadrant.DO,
        LocalTime.of(18, 0), LocalTime.of(20, 0), true, null, null, List.of(), 7L,
        Instant.EPOCH, Instant.EPOCH);
  }

  @Test
  @DisplayName("トークンなしのアクセスは401のJSONを返す（リダイレクトしない）")
  void rejectsAnonymousAccessWithJson() throws Exception {
    mockMvc.perform(get(TASKS_PATH))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value(AppErrorCode.UNAUTHENTICATED.getCode()))
        .andExpect(jsonPath("$.path").value(TASKS_PATH));
  }

  @Test
  @DisplayName("クリックジャッキングとCSPのヘッダーが付与される")
  void writesSecurityHeaders() throws Exception {
    mockMvc.perform(get(TASKS_PATH))
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(header().string("Content-Security-Policy",
            org.hamcrest.Matchers.containsString("frame-ancestors 'none'")))
        .andExpect(header().string("Referrer-Policy", "same-origin"));
  }

  @Test
  @DisplayName("作成に成功すると201とLocationヘッダーを返す")
  void createsTask() throws Exception {
    givenAuthenticated();
    when(taskService.create(any())).thenReturn(sampleTask());

    mockMvc.perform(post(TASKS_PATH)
            .header("Authorization", BEARER)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title":"請求書を作成する","description":"10月分",
                 "status":"PENDING","priority":"HIGH","dueDate":"2026-10-15"}
                """))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/tasks/1"))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  @DisplayName("バリデーション違反は400とfieldErrorsを返す")
  void rejectsInvalidPayload() throws Exception {
    givenAuthenticated();
    mockMvc.perform(post(TASKS_PATH)
            .header("Authorization", BEARER)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title":"","status":"PENDING","priority":"HIGH"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value(AppErrorCode.INVALID_REQUEST.getCode()))
        .andExpect(jsonPath("$.fieldErrors.title").exists());
  }

  @Test
  @DisplayName("タグを含むタイトルは許可リストで拒否される")
  void rejectsMarkupInTitle() throws Exception {
    givenAuthenticated();
    mockMvc.perform(post(TASKS_PATH)
            .header("Authorization", BEARER)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title":"<script>alert(1)</script>","status":"PENDING","priority":"LOW"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors.title").exists());
  }

  @Test
  @DisplayName("他人のタスクを指定した場合は404を返す")
  void returnsNotFoundForForeignTask() throws Exception {
    givenAuthenticated();
    when(taskService.findById(eq(99L)))
        .thenThrow(new AppException(AppErrorCode.RESOURCE_NOT_FOUND));

    mockMvc.perform(get(TASKS_PATH + "/99").header("Authorization", BEARER))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value(AppErrorCode.RESOURCE_NOT_FOUND.getCode()));
  }

  @Test
  @DisplayName("ステータス更新は200を返す")
  void updatesStatus() throws Exception {
    givenAuthenticated();
    when(taskService.updateStatus(eq(1L), eq(TaskStatus.COMPLETED))).thenReturn(sampleTask());

    mockMvc.perform(patch(TASKS_PATH + "/1/status")
            .header("Authorization", BEARER)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"COMPLETED\"}"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("削除は204を返す")
  void deletesTask() throws Exception {
    givenAuthenticated();
    mockMvc.perform(delete(TASKS_PATH + "/1").header("Authorization", BEARER))
        .andExpect(status().isNoContent());
    verify(taskService).delete(1L);
  }
}
