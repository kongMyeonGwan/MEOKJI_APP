// 로그인 토큰을 브라우저에 저장하고 꺼내는 함수들.
// useState 는 새로고침하면 사라지기 때문에 localStorage 를 쓴다.

const TOKEN_KEY = 'meokji.accessToken'

// 저장된 토큰 꺼내기. 없으면 null
export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

// 로그인 성공했을 때 저장
export function saveToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

// 로그아웃할 때 삭제
export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}
