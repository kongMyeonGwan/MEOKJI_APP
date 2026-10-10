// 백엔드에 요청을 보내는 axios 객체.
// api 폴더의 모든 파일이 이걸 가져다 쓴다. (import client from './client')
//
// 왜 한 곳에 모았나
//
// 주소와 시간 제한을 파일마다 적으면, 서버 주소를 바꿀 때 열두 군데를
// 고쳐야 한다. 한 군데를 빠뜨리면 그 기능만 옛 주소로 요청을 보낸다.
//
// 왜 fetch 가 아니라 axios 인가
//
// fetch 는 404 · 500 도 "성공" 으로 보고, 응답 본문을 읽으려면 await 를
// 한 번 더 해야 한다(res.json()). axios 는 400 이상이면 catch 로 떨어지고
// 본문도 response.data 에 담겨 와서, try-catch 한 벌로 끝난다.
//
// 토큰은 여기 붙이지 않는다
//
// 요청마다 headers: authHeader() 를 따로 넘긴다. 인터셉터로 한 번에
// 붙이는 방법도 있지만, 그러면 "이 요청이 로그인을 쓰는지" 가 코드에
// 안 보인다. 로그인 없이도 되는 주소(목록 · 상세)가 많아서 구별이 중요하다.

import axios from 'axios'

const client = axios.create({
  // 백엔드 주소. .env 파일의 VITE_API_BASE_URL 값을 쓰고, 없으면 스프링 서버(8090)
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8090',
  // 30초 안에 응답이 없으면 실패로 본다 (레시피 만들기가 오래 걸릴 수 있어서 길게 잡았다)
  //
  // 레시피는 재료마다 시세 API 를 부른다. 재료가 아홉 개면 6초쯤 걸리고,
  // 그쪽이 느린 날은 더 걸린다. 기본값(제한 없음)으로 두면 서버가
  // 멈췄을 때 화면이 영원히 "불러오는 중" 으로 남는다
  timeout: 30000,
})

export default client
