package com.github.reidn3r.async_multithreading.dto.HealthCheck;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class HealthCheckDTO {
  public boolean isAlive;
  public HttpStatus statusCode;
  public String instance;

  public HealthCheckDTO(@Value("${health.instance:1}") String instance){
    this.isAlive = true;
    this.statusCode = HttpStatus.OK;
    this.instance = instance;
  }
}
