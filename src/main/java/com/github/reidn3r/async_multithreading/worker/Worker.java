package com.github.reidn3r.async_multithreading.worker;

import java.util.Map;
import java.util.concurrent.Executor;

import org.springframework.stereotype.Component;

import com.github.reidn3r.async_multithreading.services.DbService;

import io.lettuce.core.StreamMessage;

@Component
public class Worker {
  private final Executor threadExecutor;
  private final DbService pgsql;

  public Worker(Executor threadExecutor, DbService db) {
    this.threadExecutor = threadExecutor;
    this.pgsql = db;
  }

  public void process(StreamMessage<String, String> message){
    threadExecutor.execute(() -> {
      Map<String, String> body = message.getBody();
                
      Long postId = Long.valueOf(body.get("postId"));
      Long userId = Long.valueOf(body.get("userId"));
      String interaction = body.get("interaction");

      pgsql.save(userId, postId, interaction);      
    });
  }
  }
