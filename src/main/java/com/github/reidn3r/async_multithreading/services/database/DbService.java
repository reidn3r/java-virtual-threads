package com.github.reidn3r.async_multithreading.services.database;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionPostDTO;
import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionPostStats;

@Service
public class DbService {
  private final FindPostService findPostService;
  private final StatsService statsService;
  private final BatchInsertService batchInsertService;
  private final UpdateCounterService updateCounterService;

  public DbService(
    FindPostService findPostService,
    StatsService statsService,
    BatchInsertService batchInsertService,
    UpdateCounterService updateCounterService
  ){
    this.findPostService = findPostService;
    this.statsService = statsService;
    this.batchInsertService = batchInsertService;
    this.updateCounterService = updateCounterService;
  }

  public InteractionPostDTO findPostById(Long id){
    return this.findPostService.run(id);
  }

  public InteractionPostStats stats() {
    return this.statsService.run();
  }

  public void batchInsert(String tablename, Map<String, String> items) {
    this.batchInsertService.run(tablename, items);
  }

  public void updateInteractionsCounterBatch(Map<Long, Long> counts, String column) {
    this.updateCounterService.run(counts, column);
  }
}
