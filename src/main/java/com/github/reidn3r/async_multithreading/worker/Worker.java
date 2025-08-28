package com.github.reidn3r.async_multithreading.worker;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.lettuce.core.Consumer;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.XReadArgs.StreamOffset;
import io.lettuce.core.api.sync.RedisCommands;

@Component
public class Worker {
	private static final String REDIS_STREAM = "interactions_stream";
	private static final String CONSUMER_GROUP = "interactions_group";
	private static final String CONSUMER_NAME = "consumer-" + UUID.randomUUID();
	private static final String POSTS_HASH = "posts:stats";
	private static final String USER_LIKE_PREFIX = "pending:user_like:";
	private static final String USER_SHARE_PREFIX = "pending:user_share:";

	private static final int MAX_BATCH = 5000;
	private static final int POLL_TIMEOUT = 50;

	private final RedisCommands<String, String> syncRedis;
	private final Executor dbExecutor;
	private final Executor redisExecutor;
	private final JdbcTemplate template;

	public Worker(
		@Qualifier("redisExecutor") Executor redisExecutor,
		RedisCommands<String, String> syncRedis,
		@Qualifier("dbExecutor") Executor dbExecutor,
		JdbcTemplate jdbcTemplate
	) {
		this.dbExecutor = dbExecutor;
		this.redisExecutor = redisExecutor;
		this.syncRedis = syncRedis;
		this.template = jdbcTemplate;
	}

	@Scheduled(fixedDelay = 10)
	public void consume() {
		List<StreamMessage<String, String>> messages = syncRedis.xreadgroup(
			Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
			XReadArgs.Builder.block(POLL_TIMEOUT).count(MAX_BATCH),
			StreamOffset.lastConsumed(REDIS_STREAM)
		);
		if (messages != null && !messages.isEmpty()) {
			this.processStreamMessageBatch(messages);
		}
	}

	@Scheduled(fixedDelay = 10_000)
	public void pgsqlBatchInsert(){
		dbExecutor.execute(() -> {
			processBatchForTable("tb_likes", USER_LIKE_PREFIX);
			processBatchForTable("tb_shares", USER_SHARE_PREFIX);
		});
	}

	private void processBatchForTable(String tableName, String redisPrefix) {
    String cursor = "0";
    Map<String, String> records = new HashMap<>();
    
    do {
			KeyScanCursor<String> result = this.syncRedis.scan(
				ScanCursor.of(cursor), 
				ScanArgs.Builder.matches(redisPrefix + "*")
			);
			for (String key : result.getKeys()) {
				String value = this.syncRedis.getdel(key);
				if (value != null) {
					records.put(key, value);
				}
			}

			if (!records.isEmpty()) {
				this.insert(tableName, records);
				records.clear();
			}
			
			cursor = result.getCursor();
    } while (!cursor.equals("0"));
	}

	private void insert(String tablename, Map<String, String> items) {
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
			this.template.batchUpdate(sql, batchArgs);
    }
	}

	private void processStreamMessageBatch(List<StreamMessage<String, String>> messages) {
		redisExecutor.execute(() -> {
			Map<Long, Long> likes = new HashMap<>();
			Map<Long, Long> shares = new HashMap<>();

			for (StreamMessage<String, String> msg : messages) {
				Long postId = Long.parseLong(msg.getBody().get("postId"));
				Long userId = Long.parseLong(msg.getBody().get("userId"));
				String interaction = msg.getBody().get("interaction");

				if ("INCREMENT_LIKE".equals(interaction)) {
					String userLikeKey = USER_LIKE_PREFIX + userId + "::" + postId;
					boolean isNewLike = syncRedis.setnx(userLikeKey, Instant.now().toString());
					
					if (isNewLike) {
						likes.merge(postId, 1L, Long::sum);
						syncRedis.expire(userLikeKey, 60*5);
					}
				} 
				else if ("INCREMENT_SHARE".equals(interaction)) {
					String userShareKey = USER_SHARE_PREFIX + userId + "::" + postId;
					boolean isNewShare = syncRedis.setnx(userShareKey, Instant.now().toString());
						
				if (isNewShare) {
					shares.merge(postId, 1L, Long::sum);
					syncRedis.expire(userShareKey, 60*5); 
				}
			}
		}
		this.updateInteractionsCounterBatch(likes, "likes_count");
		this.updateInteractionsCounterBatch(shares, "shares_count");
		this.updateRedisCache(likes, shares);
		
		// Ack em lote
		String[] messageIds = messages.stream()
			.map(StreamMessage::getId)
			.toArray(String[]::new);
			
		syncRedis.xack(REDIS_STREAM, CONSUMER_GROUP, messageIds);
		});
	}
    
	private void updateInteractionsCounterBatch(Map<Long, Long> counts, String column) {
		if (counts.isEmpty()) return;
			
		// UPDATE em lote
		String sql = "UPDATE tb_posts SET " + column + " = " + column + " + ? WHERE id = ?";
		
		List<Object[]> params = counts.entrySet().stream()
			.map(entry -> new Object[]{entry.getValue(), entry.getKey()})
			.collect(Collectors.toList());
		
		this.template.batchUpdate(sql, params);
	}

	private void updateRedisCache(Map<Long, Long> likes, Map<Long, Long> shares) {
		redisExecutor.execute(() -> {            
			likes.forEach((postId, count) -> {
				String key = "post:" + postId + ":likes";
				syncRedis.hincrby(POSTS_HASH, key, count);
			});	
			shares.forEach((postId, count) -> {
				String key = "post:" + postId + ":shares";  
				syncRedis.hincrby(POSTS_HASH, key, count);
			});
		});
	}
}