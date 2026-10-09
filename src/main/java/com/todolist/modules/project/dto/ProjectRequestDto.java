package com.todolist.modules.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Create and update input for a project.
 *
 * @param name display name, unique per account
 * @param color hex colour such as {@code #6366F1}
 */
public record ProjectRequestDto(

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{L}\\p{N}\\p{Zs}\\p{P}\\p{Sm}\\p{Sc}&&[^<>]]+$",
        message = "使用できない文字が含まれています")
    String name,

    @NotBlank
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "#RRGGBB 形式で指定してください")
    String color) {
}
