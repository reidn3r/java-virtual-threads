package com.github.reidn3r.async_multithreading.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.github.reidn3r.async_multithreading.domain.PostEntity;
import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionPostDTO;
import com.github.reidn3r.async_multithreading.dto.Interaction.InteractionPostStats;
import com.github.reidn3r.async_multithreading.repository.PostsRepository;

import io.lettuce.core.api.sync.RedisCommands;

@Service
public class DbService {
  private final PostsRepository postsRepository;
  private final RedisCommands<String, String> redisCommands;

  private static final String USER_LIKE_PREFIX = "pending:user_like:";
	private static final String USER_SHARE_PREFIX = "pending:user_share:";
  private JdbcTemplate jdbcTemplate;

  public DbService(
    PostsRepository postsRepository,
    RedisCommands<String, String> redisCommands,
    JdbcTemplate jdbcTemplate
  ){
    this.postsRepository = postsRepository;
    this.redisCommands = redisCommands;
    this.jdbcTemplate = jdbcTemplate;
  }

  public InteractionPostDTO findPostById(Long id){
    Map<String, String> redisStats = redisCommands.hgetall("post:" + id);
    if (redisStats != null && !redisStats.isEmpty()) {
      Long likes = Long.parseLong(redisStats.getOrDefault("likes", "0"));
      Long shares = Long.parseLong(redisStats.getOrDefault("shares", "0"));
      return new InteractionPostDTO(id, likes, shares);
    }

    Optional<PostEntity> foundPost = this.postsRepository.findById(id);
    if(foundPost.isPresent()){
      return new InteractionPostDTO(
        id,
        foundPost.get().getLikes_count(),
        foundPost.get().getShares_count()
      );
    }

    return null;
  }

  public InteractionPostStats stats() {
    return this.postsRepository.stats();
  }

  public void batchInsert(String tablename, Map<String, String> items) {
    if (items == null || items.isEmpty()) return;
  
    String sql = "INSERT INTO " + tablename + " (user_id, post_id) VALUES (?, ?) ON CONFLICT (user_id, post_id) DO NOTHING";
    
    List<Object[]> batchArgs = new ArrayList<>();
    String prefix = tablename.equals("tb_likes") ? USER_LIKE_PREFIX : USER_SHARE_PREFIX;
    
    for (String key : items.keySet()) {
			try {
				// Remover o prefixo e extrair userId e postId
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

	public void updateInteractionsCounterBatch(Map<Long, Long> counts, String column) {
		if (counts.isEmpty()) return;
			
		// UPDATE em lote
		String sql = "UPDATE tb_posts SET " + column + " = " + column + " + ? WHERE id = ?";
		
		List<Object[]> params = counts.entrySet().stream()
			.map(entry -> new Object[]{entry.getValue(), entry.getKey()})
			.collect(Collectors.toList());
		
		this.jdbcTemplate.batchUpdate(sql, params);
	}
}
