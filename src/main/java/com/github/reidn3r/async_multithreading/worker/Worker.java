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

    private static final int MAX_BATCH = 5000;
    private static final int POLL_TIMEOUT = 50;

    private final RedisCommands<String, String> syncRedis;
    private final Executor dbExecutor;
    private JdbcTemplate template;

    public Worker(
        RedisCommands<String, String> syncRedis,
        @Qualifier("dbExecutor") Executor dbExecutor
    ) {
        this.syncRedis = syncRedis;
        this.dbExecutor = dbExecutor;
    }

    @Scheduled(fixedDelay = 10)
    public void consumeInteractions() {
        try {
            List<StreamMessage<String, String>> messages = syncRedis.xreadgroup(
                Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                XReadArgs.Builder.block(POLL_TIMEOUT).count(MAX_BATCH),
                StreamOffset.lastConsumed(REDIS_STREAM)
            );
            
            if (messages != null && !messages.isEmpty()) {
                processBatchAsync(messages);
            }
        } catch (Exception e) {
            System.err.println("Consumer error: " + e.getMessage());
        }
    }

    private void processBatchAsync(List<StreamMessage<String, String>> messages) {
        dbExecutor.execute(() -> {
            long start = System.currentTimeMillis();
            
            try {
                Map<Long, Long> likes = new HashMap<>();
                Map<Long, Long> shares = new HashMap<>();
                
                // Processamento ULTRA rápido
                for (StreamMessage<String, String> msg : messages) {
                    Long postId = Long.parseLong(msg.getBody().get("postId"));
                    String interaction = msg.getBody().get("interaction");
                    
                    if ("INCREMENT_LIKE".equals(interaction)) {
                        likes.merge(postId, 1L, Long::sum);
                    } else {
                        shares.merge(postId, 1L, Long::sum);
                    }
                }
                
                // Batch update NOVO (mais rápido)
                updateCountsBatch(likes, "likes_count");
                updateCountsBatch(shares, "shares_count");
                
                // Ack em lote (MUITO mais rápido)
                String[] messageIds = messages.stream()
                    .map(StreamMessage::getId)
                    .toArray(String[]::new);
                
                syncRedis.xack("interactions_stream", "interactions_group", messageIds);
                
                System.out.println("Processed " + messages.size() + " messages in " + 
                                  (System.currentTimeMillis() - start) + "ms");
                
            } catch (Exception e) {
                System.err.println("Batch processing error: " + e.getMessage());
            }
        });
    }
    
    private void updateCountsBatch(Map<Long, Long> counts, String column) {
        if (counts.isEmpty()) return;
        
        // UPDATE em lote MUITO rápido
        String sql = "UPDATE tb_posts SET " + column + " = " + column + " + ? WHERE id = ?";
        
        List<Object[]> params = counts.entrySet().stream()
            .map(entry -> new Object[]{entry.getValue(), entry.getKey()})
            .collect(Collectors.toList());
        
        template.batchUpdate(sql, params);
    }

}