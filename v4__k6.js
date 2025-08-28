import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend, Rate } from 'k6/metrics';

const NUM_POSTS = 1000;

export const options = {
  stages: [
    { duration: '15s', target: 200 },
    { duration: '30s', target: 200 },
    { duration: '30s', target: 500 },
    { duration: '1m', target: 500 },
    { duration: '30s', target: 700 },
    { duration: '1m', target: 700 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<100'], // Relaxado para 100ms
    'successful_requests': ['rate>0.95'],
  },
};

const interactions = new Counter('total_interactions');
const like_sent = new Counter('likes_sent');
const share_sent = new Counter('shares_sent');
const durations = new Trend('interaction_duration');
const successRate = new Rate('successful_requests');

export default function () {
  const url = 'http://localhost:9999/interactions';

  const interactionType = Math.random() < 0.5 ? 'INCREMENT_LIKE' : 'INCREMENT_SHARE';
  
  const payload = JSON.stringify({
    userId: Math.floor(Math.random() * NUM_POSTS) + 1,
    postId: Math.floor(Math.random() * NUM_POSTS) + 1,
    interaction: interactionType
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
    timeout: '30s',
  };

  const res = http.post(url, payload, params);
  
  durations.add(res.timings.duration);

  if (interactionType === 'INCREMENT_LIKE'){
    like_sent.add(1)
  } else {
    share_sent.add(1);
  }

  interactions.add(1);
  successRate.add(res.status === 201);

  check(res, {
    'status 201': (r) => r.status === 201,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(0.5); 
}