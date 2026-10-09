package com.todolist.shared.web;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Stable pagination envelope.
 *
 * <p>Spring's own page implementations are not a documented serialisation contract, so responses
 * use this instead.
 *
 * @param content rows on the requested page
 * @param page zero based page index
 * @param size requested page size
 * @param totalElements total row count across all pages
 * @param totalPages total page count
 * @param last whether this is the final page
 * @param <T> row type
 */
public record PagedResponse<T>(List<T> content, int page, int size, long totalElements,
    int totalPages, boolean last) {

  /**
   * @param source page returned by Spring Data
   * @param <T> row type
   * @return envelope carrying the same rows and counters
   */
  public static <T> PagedResponse<T> from(Page<T> source) {
    return new PagedResponse<>(
        source.getContent(),
        source.getNumber(),
        source.getSize(),
        source.getTotalElements(),
        source.getTotalPages(),
        source.isLast()
    );
  }
}
