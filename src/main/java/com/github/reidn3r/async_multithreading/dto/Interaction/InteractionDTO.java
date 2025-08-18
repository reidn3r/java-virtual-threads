package com.github.reidn3r.async_multithreading.dto.Interaction;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InteractionDTO(
  @Min(0) int userId,
  @Min(0) int postId,
  @NotNull() InteractionEnum interaction
) {}
