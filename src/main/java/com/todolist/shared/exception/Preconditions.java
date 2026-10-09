package com.todolist.shared.exception;

import java.util.Collection;
import org.apache.commons.lang3.StringUtils;

/**
 * Guard clauses that fail with {@link AppException} instead of JDK runtime exceptions.
 */
public final class Preconditions {

  private Preconditions() {
  }

  /**
   * @param value value to test
   * @param errorCode error raised when the value is {@code null}
   * @param <T> value type
   * @return the value, guaranteed non-null
   * @throws AppException when the value is {@code null}
   */
  public static <T> T requireNonNull(T value, AppErrorCode errorCode) {
    if (value == null) {
      throw new AppException(errorCode);
    }
    return value;
  }

  /**
   * @param value value to test
   * @param errorCode error raised when the value is null, empty or whitespace only
   * @return the value, guaranteed to hold non-whitespace characters
   * @throws AppException when the value is blank
   */
  public static String requireNotBlank(String value, AppErrorCode errorCode) {
    if (StringUtils.isBlank(value)) {
      throw new AppException(errorCode);
    }
    return value;
  }

  /**
   * @param value collection to test
   * @param errorCode error raised when the collection is null or empty
   * @param <T> element type
   * @return the collection, guaranteed to hold at least one element
   * @throws AppException when the collection is empty
   */
  public static <T> Collection<T> requireNotEmpty(Collection<T> value, AppErrorCode errorCode) {
    if (value == null || value.isEmpty()) {
      throw new AppException(errorCode);
    }
    return value;
  }

  /**
   * @param condition condition that must hold
   * @param errorCode error raised when the condition is {@code false}
   * @throws AppException when the condition does not hold
   */
  public static void checkState(boolean condition, AppErrorCode errorCode) {
    if (!condition) {
      throw new AppException(errorCode);
    }
  }
}
