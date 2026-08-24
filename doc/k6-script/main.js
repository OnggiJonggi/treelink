// 메인 화면

import http from 'k6/http';
import { sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';


export const options = {
  vus: 10, // 동시 요청 수
  duration: '30s', // 지속 시간
};

export default function () {
  http.get('https://treelink.website/');
  sleep(1); // 1초 대기
}



export function handleSummary(data) {
  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    'summary.json': JSON.stringify(data, null, 2),
  };
}