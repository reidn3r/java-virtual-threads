package com.github.reidn3r.async_multithreading.services.stream;

import java.util.HashMap;

import org.springframework.stereotype.Service;

import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionDTO;

@Service
public class BuildStreamPayloadService {
  private static final String FIELD_POST_ID = "postId";
  private static final String FIELD_USER_ID = "userId";
  private static final String FIELD_INTERACTION = "interaction";

  public HashMap<String, String> run(InteractionDTO data) {
    HashMap<String, String> payload = new HashMap<String, String>();
    String postId = Integer.toString(data.postId());
    String userId = Integer.toString(data.userId());

    payload.put(FIELD_POST_ID, postId);
    payload.put(FIELD_USER_ID, userId);
    payload.put(FIELD_INTERACTION, data.interaction().getInteraction());
    return payload;
  }
}
