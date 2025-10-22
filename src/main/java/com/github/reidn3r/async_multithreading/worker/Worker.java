package com.github.reidn3r.async_multithreading.worker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.github.reidn3r.async_multithreading.services.DbService;
import com.github.reidn3r.async_multithreading.services.interaction.InteractionFactory;
import com.github.reidn3r.async_multithreading.services.interaction.InteractionStrategy;

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
	private final Executor redisExecutor;
	private final InteractionFactory interactionFactory;
	private final Executor writeExecutor;
	private final DbService dbService;

	public Worker(
		@Qualifier("redisExecutor") Executor redisExecutor,
		RedisCommands<String, String> syncRedis,
		@Qualifier("writeExecutor") Executor writeExecutor,
		DbService dbService,
		InteractionFactory interactionStrategyFactory
	) {
		this.writeExecutor = writeExecutor;
		this.redisExecutor = redisExecutor;
		this.syncRedis = syncRedis;
		this.interactionFactory = interactionStrategyFactory;
		this.dbService = dbService;
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
    redisExecutor.execute(() -> {
			Map<Long, Long> likes = new HashMap<>();
			Map<Long, Long> shares = new HashMap<>();

			for (StreamMessage<String, String> msg : messages) {
				Long postId = Long.parseLong(msg.getBody().get("postId"));
				Long userId = Long.parseLong(msg.getBody().get("userId"));
				String interaction = msg.getBody().get("interaction");

				InteractionStrategy interactionHandler = this.interactionFactory.getInteractionStrategy(interaction);
				boolean isNewRecord = interactionHandler.handle(postId, userId, interaction, syncRedis);

				if(isNewRecord){
					Map<Long, Long> map = interaction.equals("INCREMENT_LIKE") ? likes : shares;
					map.merge(postId, 1L, Long::sum);
				}
			}
      
			this.runPgsqlUpdate(likes, shares);
			this.updateRedisCache(likes, shares);
        
			// Ack em lote
			String[] messageIds = messages.stream()
				.map(StreamMessage::getId)
				.toArray(String[]::new);

			syncRedis.xack(REDIS_STREAM, CONSUMER_GROUP, messageIds);
    }); 
	}

	private void runPgsqlUpdate(Map<Long, Long> likes, Map<Long, Long> shares){
		if (likes.isEmpty() && shares.isEmpty()) return;
    writeExecutor.execute(() -> {
			this.dbService.updateInteractionsCounterBatch(likes, "likes_count");
			this.dbService.updateInteractionsCounterBatch(shares, "shares_count");
    });
	}
    
	@Scheduled(fixedDelay = 10_000)
	public void pgsqlBatchInsert(){
		writeExecutor.execute(() -> {
			this.processBatchForTable("tb_likes", USER_LIKE_PREFIX);
			this.processBatchForTable("tb_shares", USER_SHARE_PREFIX);
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
				this.dbService.batchInsert(tableName, records);
				records.clear();
			}
			
			cursor = result.getCursor();
    } while (!cursor.equals("0"));
	}

	private void updateRedisCache(Map<Long, Long> likes, Map<Long, Long> shares) {
		likes.forEach((postId, count) -> {
			String key = "post:" + postId + ":likes";
			syncRedis.hincrby(POSTS_HASH, key, count);
		});	
		shares.forEach((postId, count) -> {
			String key = "post:" + postId + ":shares";  
			syncRedis.hincrby(POSTS_HASH, key, count);
		});
	}
}