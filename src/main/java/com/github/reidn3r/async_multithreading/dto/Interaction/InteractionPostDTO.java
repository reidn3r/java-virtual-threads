package com.github.reidn3r.async_multithreading.dto.Interaction;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InteractionPostDTO {
  public Long postId;
  public Long likeCount;
  public Long shareCount;
}
