import client from './client'

// 백엔드랑 DB 가 살아있는지 확인한다
//
// 마이페이지 아래의 "개발용 연결 상태" 카드가 부른다.
//
// 화면이 안 뜰 때 원인이 셋이다 — 프론트가 안 떴거나, 서버가 안 떴거나,
// DB 가 안 됐거나. 이 함수가 값을 돌려주면 앞의 둘은 괜찮다는 뜻이고,
// database 칸이 셋째를 알려준다.
export const fetchHealth = async () => {
  try {
    const response = await client.get('/api/health')
    return response.data
  } catch (err) {
    // DB 가 죽었을 때(503)도 서버가 상태 정보를 보내준다
    //
    // axios 는 400 이상을 에러로 보고 catch 로 떨어뜨린다. 그런데 이
    // 주소는 503 일 때도 본문에 쓸 만한 값을 담아 보낸다 - 그것이
    // 이 함수가 알아내려는 것이다
    if (err.response) {
      return err.response.data
    }
    // err.response 가 없으면 서버에 아예 닿지 못한 것이다
    // (서버가 꺼졌거나 주소가 틀렸거나). 그때는 올려보낸다 -
    // "DB 가 안 된다" 와 "서버가 없다" 는 다른 얘기다
    throw err
  }
}
