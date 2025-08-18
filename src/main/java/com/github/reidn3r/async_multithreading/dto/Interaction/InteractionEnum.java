package com.github.reidn3r.async_multithreading.dto.Interaction;

public enum InteractionEnum {
  INCREMENT_LIKE("INCREMENT_LIKE"),
  INCREMENT_SHARE("INCREMENT_SHARE");

  private String interaction;

  InteractionEnum(String interaction){
    this.interaction = interaction;
  }

  public String getInteraction(){
    return this.interaction;
  }
}
