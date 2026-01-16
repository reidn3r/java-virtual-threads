package com.github.reidn3r.async_multithreading.services.database;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class UpdateCounterService {
  private final JdbcTemplate jdbcTemplate;

  public UpdateCounterService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void run(Map<Long, Long> counts, String column) {
    if (counts.isEmpty()) return;
    
    String sql = "UPDATE tb_posts SET " + column + " = " + column + " + ? WHERE id = ?";
    
    List<Object[]> params = counts.entrySet().stream()
      .map(entry -> new Object[]{entry.getValue(), entry.getKey()})
      .collect(Collectors.toList());
    
    this.jdbcTemplate.batchUpdate(sql, params);
  }
}
