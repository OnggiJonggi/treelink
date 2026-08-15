// 메인 화면

import http from 'k6/http';
import { sleep } from 'k6';

export const options = {
  vus: 10, // 동시 요청 수
  duration: '30s', // 지속 시간
};

export default function () {
  http.get('https://treelink.website/');
  sleep(1); // 1초 대기
}