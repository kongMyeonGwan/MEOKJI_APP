import client from './client'
import { authHeader } from '../utils/token'

// 못 먹는 재료 API. 로그인이 필요하다.
//
// 여기 등록한 재료는 추천과 레시피에서 빠진다. 서버가 알아서 빼주므로
// 화면에서 걸러낼 필요가 없다.
//
// 세 개뿐이고 수정이 없다. 재료 이름을 고치는 일은 "지우고 다시 넣기" 와
// 같아서 화면에서도 그렇게 쓴다.

// 내 못 먹는 재료 목록
export const getBans = async () => {
  const response = await client.get('/api/bans', { headers: authHeader() })
  return response.data
}

// 추가. 이미 등록한 재료면 409 에러가 난다.
export const addBan = async (form) => {
  const response = await client.post(
    '/api/bans',
    {
      ingredient_name: form.ingredientName,
      ban_type: form.banType,
    },
    { headers: authHeader() },
  )
  return response.data
}

// 삭제
//
// return 이 없다. 서버가 204(내용 없음)를 주기 때문에 받을 값이 없다.
// 다른 함수들처럼 response.data 를 돌려주면 undefined 가 간다.
//
// 성공하면 조용히 끝나고, 남의 것을 지우려 하면 404 가 던져진다
export const removeBan = async (id) => {
  await client.delete(`/api/bans/${id}`, { headers: authHeader() })
}
