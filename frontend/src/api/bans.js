import client from './client'

// 못 먹는 재료 API. 로그인이 필요하다.

// 내 못 먹는 재료 목록
export const getBans = async () => {
  const response = await client.get('/api/bans')
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
  )
  return response.data
}

// 삭제
export const removeBan = async (id) => {
  await client.delete(`/api/bans/${id}`)
}
