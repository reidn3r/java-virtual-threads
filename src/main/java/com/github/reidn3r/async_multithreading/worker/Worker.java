package com.github.reidn3r.async_multithreading.worker;

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
	private static final String USER_LIKE_PREFIX = "user_like:";
	private static final String USER_SHARE_PREFIX = "user_share:";

	private static final int MAX_BATCH = 5000;
	private static final int POLL_TIMEOUT = 50;

	private final RedisCommands<String, String> syncRedis;
	private final Executor dbExecutor;
	private final Executor redisExecutor;
	private final JdbcTemplate template;

	public Worker(
		RedisCommands<String, String> syncRedis,
		JdbcTemplate jdbcTemplate,
		@Qualifier("dbExecutor") Executor dbExecutor,
		@Qualifier("redisExecutor") Executor redisExecutor
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

	private void processStreamMessageBatch(List<StreamMessage<String, String>> messages) {
		dbExecutor.execute(() -> {
			Map<Long, Long> likes = new HashMap<>();
			Map<Long, Long> shares = new HashMap<>();

			for (StreamMessage<String, String> msg : messages) {
				Long postId = Long.parseLong(msg.getBody().get("postId"));
				Long userId = Long.parseLong(msg.getBody().get("userId"));
				String interaction = msg.getBody().get("interaction");

				if ("INCREMENT_LIKE".equals(interaction)) {
					String userLikeKey = USER_LIKE_PREFIX + userId + "::" + postId;
					boolean isNewLike = syncRedis.setnx(userLikeKey, "LIKE");
					
					if (isNewLike) {
						likes.merge(postId, 1L, Long::sum);
						syncRedis.expire(userLikeKey, 60*5);
					}
				} 
				else if ("INCREMENT_SHARE".equals(interaction)) {
					String userShareKey = USER_SHARE_PREFIX + userId + "::" + postId;
					boolean isNewShare = syncRedis.setnx(userShareKey, "SHARE");
						
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