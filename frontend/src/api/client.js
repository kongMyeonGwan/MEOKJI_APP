// 백엔드에 요청을 보내는 axios 객체.
// api 폴더의 모든 파일이 이걸 가져다 쓴다. (import client from './client')

import axios from 'axios'

const client = axios.create({
  // 백엔드 주소. .env 파일의 VITE_API_BASE_URL 값을 쓰고, 없으면 스프링 서버(8080)
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  // 30초 안에 응답이 없으면 실패로 본다 (레시피 만들기가 오래 걸릴 수 있어서 길게 잡았다)
  timeout: 30000,
})

export default client
