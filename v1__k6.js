import { textSummary } from "https://jslib.k6.io/k6-summary/0.1.0/index.js";
import { check, sleep } from "k6";
import { Counter, Rate, Trend } from "k6/metrics";
import http from "k6/http";

// ========== CONFIGURAÇÕES DO TESTE ==========
const BASE_URL = __ENV.BASE_URL || "http://localhost:9999";
const MAX_USERS = parseInt(__ENV.MAX_USERS || "1000");
const MAX_POSTS = parseInt(__ENV.MAX_POSTS || "500");
const TEST_DURATION = __ENV.TEST_DURATION || "120s";

// ========== MÉTRICAS CUSTOMIZADAS ==========
const interactionsSuccess = new Counter("interactions_success");
const interactionsFailure = new Counter("interactions_failure");
const postsReadSuccess = new Counter("posts_read_success");
const postsReadFailure = new Counter("posts_read_failure");
const consistencyErrors = new Counter("consistency_errors");
const interactionProcessingDelay = new Trend("interaction_processing_delay");

// Contadores por tipo de interação
const likesProcessed = new Counter("likes_processed");
const sharesProcessed = new Counter("shares_processed");

// Taxa de sucesso
const successRate = new Rate("success_rate");

export const options = {
  summaryTrendStats: ["p50", "p95", "p99", "p99.9", "avg", "max"],
  thresholds: {
    // Performance thresholds
    'http_req_duration{endpoint:interactions}': ['p99 < 5', 'p95 < 3'],
    'http_req_duration{endpoint:posts}': ['p99 < 3', 'p95 < 2'],
    'http_req_failed': ['rate < 0.01'],
    
    // Business thresholds
    'consistency_errors': ['count == 0'],
    'success_rate': ['rate > 0.99'],
    'interactions_success': ['count > 10000'], // Ajuste conforme necessário
  },
  
  scenarios: {
    // ========== CENÁRIO PRINCIPAL: INTERAÇÕES DE USUÁRIOS ==========
    user_interactions: {
      exec: "userInteractions",
      executor: "ramping-vus",
      startVUs: 10,
      gracefulRampDown: "10s",
      stages: [
        { target: 50, duration: "30s" },   // Aquecimento
        { target: 200, duration: "30s" },  // Carga moderada
        { target: 500, duration: "30s" },  // Carga alta
        { target: 800, duration: "20s" },  // Pico
        { target: 200, duration: "10s" },  // Cooldown
      ],
      tags: { scenario: "interactions" }
    },

    // ========== CENÁRIO: LEITURAS DE POSTS ==========
    posts_reading: {
      exec: "postsReading",
      executor: "constant-vus",
      vus: 30,
      duration: TEST_DURATION,
      startTime: "10s", // Começa após as interações iniciarem
      tags: { scenario: "reading" }
    },

    // ========== CENÁRIO: VERIFICAÇÃO DE CONSISTÊNCIA ==========
    consistency_check: {
      exec: "consistencyCheck",
      executor: "constant-vus",
      vus: 1,
      duration: TEST_DURATION,
      startTime: "15s", // Começa após ter dados para verificar
      tags: { scenario: "consistency" }
    },

    // ========== CENÁRIO: BURST DE CARGA ==========
    burst_load: {
      exec: "burstLoad",
      executor: "per-vu-iterations",
      vus: 100,
      iterations: 5,
      startTime: "60s", // Burst no meio do teste
      maxDuration: "10s",
      tags: { scenario: "burst" }
    },

    // ========== CENÁRIO: TESTE DE RESISTÊNCIA ==========
    stress_test: {
      exec: "stressTest",
      executor: "ramping-arrival-rate",
      startRate: 100,
      timeUnit: "1s",
      preAllocatedVUs: 200,
      maxVUs: 1000,
      stages: [
        { target: 500, duration: "20s" },  // Subida agressiva
        { target: 1000, duration: "15s" }, // Stress máximo
        { target: 100, duration: "15s" },  // Volta ao normal
      ],
      startTime: "90s", // Final do teste principal
      tags: { scenario: "stress" }
    }
  }
};

// ========== SETUP INICIAL ==========
export function setup() {
  console.log("🚀 Iniciando testes de carga do sistema de interações");
  console.log(`📊 Configuração: ${MAX_USERS} usuários, ${MAX_POSTS} posts`);
  
  // Verificar se o serviço está respondendo
  const healthCheck = http.get(`${BASE_URL}/health`, { timeout: '10s' });
  if (healthCheck.status !== 200) {
    console.error("❌ Serviço não está disponível para testes");
    return { healthy: false };
  }
  
  return { 
    healthy: true,
    startTime: new Date().toISOString()
  };
}

// ========== FUNÇÕES AUXILIARES ==========
function getRandomUserId() {
  return Math.floor(Math.random() * MAX_USERS) + 1;
}

function getRandomPostId() {
  return Math.floor(Math.random() * MAX_POSTS) + 1;
}

function getRandomInteraction() {
  return Math.random() < 0.6 ? "INCREMENT_LIKE" : "INCREMENT_SHARE";
}

function makeInteractionRequest(payload) {
  const response = http.post(
    `${BASE_URL}/interactions`,
    JSON.stringify(payload),
    {
      headers: {
        'Content-Type': 'application/json',
      },
      tags: { endpoint: 'interactions' },
      timeout: '5s'
    }
  );
  
  return response;
}

function getPostData(postId) {
  const response = http.get(
    `${BASE_URL}/posts/${postId}`,
    {
      tags: { endpoint: 'posts' },
      timeout: '3s'
    }
  );
  
  return response;
}

// ========== CENÁRIOS DE TESTE ==========

// Cenário principal: Interações de usuários
export function userInteractions() {
  const userId = getRandomUserId();
  const postId = getRandomPostId();
  const interaction = getRandomInteraction();
  
  const payload = {
    userId: userId,
    postId: postId,
    interaction: interaction
  };
  
  const response = makeInteractionRequest(payload);
  
  const success = check(response, {
    'interaction request successful': (r) => r.status >= 200 && r.status < 300,
    'response time acceptable': (r) => r.timings.duration < 10000, // 10s timeout
  });
  
  if (success) {
    interactionsSuccess.add(1);
    successRate.add(1);
    
    // Contar por tipo de interação
    if (interaction === "INCREMENT_LIKE") {
      likesProcessed.add(1);
    } else {
      sharesProcessed.add(1);
    }
  } else {
    interactionsFailure.add(1);
    successRate.add(0);
    console.warn(`❌ Falha na interação: ${response.status} - ${response.body}`);
  }
  
  // Sleep variável para simular comportamento real
  sleep(Math.random() * 2 + 0.5); // 0.5 a 2.5 segundos
}

// Cenário de leitura de posts
export function postsReading() {
  const postId = getRandomPostId();
  const response = getPostData(postId);
  
  const success = check(response, {
    'post read successful': (r) => r.status === 200,
    'post has required fields': (r) => {
      if (r.status !== 200) return false;
      try {
        const data = JSON.parse(r.body);
        return data.hasOwnProperty('id') && 
               data.hasOwnProperty('likes') && 
               data.hasOwnProperty('shares');
      } catch (e) {
        return false;
      }
    }
  });
  
  if (success) {
    postsReadSuccess.add(1);
  } else {
    postsReadFailure.add(1);
    console.warn(`❌ Falha na leitura do post ${postId}: ${response.status}`);
  }
  
  sleep(Math.random() * 1 + 0.2); // 0.2 a 1.2 segundos
}

// Cenário de verificação de consistência
export function consistencyCheck() {
  // Selecionar alguns posts para verificar consistência
  const postsToCheck = Array.from({length: 10}, () => getRandomPostId());
  let inconsistencies = 0;
  
  for (const postId of postsToCheck) {
    const response = getPostData(postId);
    
    if (response.status === 200) {
      try {
        const postData = JSON.parse(response.body);
        
        // Verificações básicas de consistência
        if (postData.likes < 0 || postData.shares < 0) {
          inconsistencies++;
          console.warn(`❌ Inconsistência: Post ${postId} com valores negativos`);
        }
        
        // Verificar se os dados fazem sentido (valores muito altos podem indicar problema)
        if (postData.likes > MAX_USERS * 2 || postData.shares > MAX_USERS * 2) {
          inconsistencies++;
          console.warn(`❌ Inconsistência: Post ${postId} com valores suspeitos`);
        }
        
      } catch (e) {
        inconsistencies++;
        console.warn(`❌ Erro ao parsear dados do post ${postId}`);
      }
    }
  }
  
  if (inconsistencies > 0) {
    consistencyErrors.add(inconsistencies);
  }
  
  sleep(10); // Verificar a cada 10 segundos
}

// Cenário de burst - rajada rápida de requisições
export function burstLoad() {
  const requests = [];
  
  // Fazer 10 requisições simultâneas
  for (let i = 0; i < 10; i++) {
    const payload = {
      userId: getRandomUserId(),
      postId: getRandomPostId(),
      interaction: getRandomInteraction()
    };
    
    requests.push(['POST', `${BASE_URL}/interactions`, JSON.stringify(payload), {
      headers: { 'Content-Type': 'application/json' },
      tags: { endpoint: 'interactions', burst: 'true' }
    }]);
  }
  
  const responses = http.batch(requests);
  
  responses.forEach((response, index) => {
    const success = response.status >= 200 && response.status < 300;
    if (success) {
      interactionsSuccess.add(1);
    } else {
      interactionsFailure.add(1);
      console.warn(`❌ Falha no burst request ${index}: ${response.status}`);
    }
  });
  
  sleep(0.1);
}

// Cenário de stress - carga máxima
export function stressTest() {
  const payload = {
    userId: getRandomUserId(),
    postId: getRandomPostId(),
    interaction: getRandomInteraction()
  };
  
  const response = makeInteractionRequest(payload);
  
  const success = response.status >= 200 && response.status < 300;
  
  if (success) {
    interactionsSuccess.add(1);
  } else {
    interactionsFailure.add(1);
  }
  
  // No stress test, sleep mínimo para pressionar o sistema
  sleep(0.01);
}

// ========== TEARDOWN E RELATÓRIO ==========
export function teardown(data) {
  console.log("🏁 Finalizando testes e coletando métricas finais");
}

export function handleSummary(data) {
  const totalInteractions = data.metrics.interactions_success.values.count || 0;
  const totalReads = data.metrics.posts_read_success.values.count || 0;
  const totalErrors = data.metrics.interactions_failure.values.count || 0;
  const inconsistencies = data.metrics.consistency_errors.values.count || 0;
  
  const p99_interactions = data.metrics['http_req_duration{endpoint:interactions}']?.values['p(99)'] || 0;
  const p99_posts = data.metrics['http_req_duration{endpoint:posts}']?.values['p(99)'] || 0;
  const p95_interactions = data.metrics['http_req_duration{endpoint:interactions}']?.values['p(95)'] || 0;
  
  const customData = {
    timestamp: new Date().toISOString(),
    test_summary: {
      total_interactions_processed: totalInteractions,
      total_post_reads: totalReads,
      total_errors: totalErrors,
      consistency_errors: inconsistencies,
      success_rate: totalInteractions / (totalInteractions + totalErrors),
    },
    performance_metrics: {
      p99_interactions_ms: Math.round(p99_interactions * 100) / 100,
      p99_posts_ms: Math.round(p99_posts * 100) / 100,
      p95_interactions_ms: Math.round(p95_interactions * 100) / 100,
      target_p99_met: p99_interactions < 5 && p99_posts < 3,
      target_p95_met: p95_interactions < 3
    },
    business_metrics: {
      likes_processed: data.metrics.likes_processed?.values.count || 0,
      shares_processed: data.metrics.shares_processed?.values.count || 0,
      interactions_per_second: totalInteractions / 120, // Assumindo 2 minutos de teste
    },
    quality_assurance: {
      consistency_check_passed: inconsistencies === 0,
      error_rate_acceptable: (totalErrors / (totalInteractions + totalErrors)) < 0.01,
      performance_targets_met: p99_interactions < 5 && p99_posts < 3
    }
  };
  
  return {
    stdout: textSummary(data, { indent: " ", enableColors: true }),
    'test-results.json': JSON.stringify(customData, null, 2),
  };
}