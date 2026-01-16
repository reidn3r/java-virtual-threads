package com.github.reidn3r.async_multithreading.services.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class BatchInsertService {
  private static final String USER_LIKE_PREFIX = "pending:user_like:";
  private static final String USER_SHARE_PREFIX = "pending:user_share:";
  private final JdbcTemplate jdbcTemplate;

  public BatchInsertService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void run(String tablename, Map<String, String> items) {
    if (items == null || items.isEmpty()) return;

    String sql = "INSERT INTO " + tablename + " (user_id, post_id) VALUES (?, ?) ON CONFLICT (user_id, post_id) DO NOTHING";
    
    List<Object[]> batchArgs = new ArrayList<>();
    String prefix = tablename.equals("tb_likes") ? USER_LIKE_PREFIX : USER_SHARE_PREFIX;
    
    for (String key : items.keySet()) {
      try {
        String idsPart = key.substring(prefix.length());
        String[] ids = idsPart.split("::");
        
        if (ids.length == 2) {
          Long userId = Long.parseLong(ids[0]);
          Long postId = Long.parseLong(ids[1]);
          batchArgs.add(new Object[]{userId, postId});
        }
      } catch (Exception e) {
        System.err.println("Erro ao processar chave: " + key);
      }
    }
    
    if (!batchArgs.isEmpty()) {
      this.jdbcTemplate.batchUpdate(sql, batchArgs);
    }
  }
}
