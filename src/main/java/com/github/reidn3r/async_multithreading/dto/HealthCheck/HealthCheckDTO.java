package com.github.reidn3r.async_multithreading.dto.HealthCheck;

import org.springframework.http.HttpStatus;

public class HealthCheckDTO {
  public boolean isAlive;
  public HttpStatus statusCode;
  public String instance;

  public HealthCheckDTO(){
    this.isAlive = true;
    this.statusCode = HttpStatus.OK;
    this.instance = "1";
  }
}
