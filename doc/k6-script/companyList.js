// /api/company 요청
import http from 'k6/http';
import { sleep } from 'k6';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import { URLSearchParams } from 'https://jslib.k6.io/url/1.0.0/index.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';


// 10분간 VU 10 -> 100 으로 서서히 증가
export const options = {
  scenarios: {
    company_search: {
      executor: 'ramping-vus',
      startVUs: 10,
      stages: [
        { duration: '9m30s', target: 100 }, // 9분 30초 동안 10 -> 100명 증가
        { duration: '30s', target: 100 },   // 마지막 30초는 100명 유지
      ],
    },
  },
  // 처음 30초(stage: warmup)는 판정에서 제외하고, main 구간만 기준으로 삼음
  thresholds: {
    'http_req_duration{stage:main}': ['p(95)<1000', 'p(99)<3000'],
    'http_req_failed{stage:main}': ['rate<0.01'],
  },
};

const BASE_URL = 'https://treelink.website/api/company';

const companyNames = ['터전', '뿌리', '조경', '가든', '초록', ''];
const representativeNames = ['미영', '배성훈', '영수', '정만', '박', '한상철', ''];
const options_ = ['벌초', '정원', '기타', '실내조명', '뭘까요', ''];

// 테스트 시작 시각을 기록해 각 요청이 warmup 구간인지 판단하는 기준으로 사용
export function setup() {
  return { startTime: Date.now() };
}

export default function (data) {
  const elapsedSec = (Date.now() - data.startTime) / 1000;
  const stage = elapsedSec < 30 ? 'warmup' : 'main';

  const companyName = randomItem(companyNames);
  const representativeName = randomItem(representativeNames);
  const option = randomItem(options_);

  const params = new URLSearchParams();
  if (companyName !== '') params.append('companyName', companyName);
  if (representativeName !== '') params.append('representativeName', representativeName);
  if (option !== '') params.append('option', option);

  const url = `${BASE_URL}?${params.toString()}`;

  const res = http.get(url, {
    tags: { stage: stage }, // warmup / main 태그 부여
  });

  console.log(`[${stage}] 상태: ${res.status}, 응답시간: ${res.timings.duration}ms, URL: ${url}`);

  sleep(1);
}


export function handleSummary(data) {
  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    'summary.json': JSON.stringify(data, null, 2),
  };
}