import client from './client'

// 못 먹는 재료(Ban) API. 전부 로그인이 필요하다.

// 내 Ban 목록. 등록한 순서대로 온다.
export async function getBans() {
  const response = await client.get('/api/bans')
  return response.data
}

// Ban 추가. 이미 등록한 재료면 409 가 난다.
export async function addBan(form) {
  const response = await client.post('/api/bans', {
    ingredient_name: form.ingredientName,
    ban_type: form.banType,
  })
  return response.data
}

// Ban 삭제. 성공하면 204 라서 돌려줄 게 없다.
export async function removeBan(id) {
  await client.delete('/api/bans/' + id)
}
