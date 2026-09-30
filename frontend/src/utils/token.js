// 로그인 정보를 브라우저에 저장하고 꺼내는 함수들.
//
// 왜 localStorage 에 저장하나?
//   useState 에 넣은 값은 새로고침하면 사라진다.
//   localStorage 는 브라우저를 껐다 켜도 남아 있어서 로그인이 유지된다.
//
// 저장하는 것 두 가지
//   토큰(token) : 서버가 로그인할 때 준 긴 글자. API 를 부를 때마다 서버에 보여준다.
//   사용자(user): 닉네임, 이메일 같은 내 정보. 화면에 보여주려고 저장한다.
//
// 개발자도구(F12) → Application → Local Storage 에서 직접 볼 수 있다.

const TOKEN_KEY = 'meokji.accessToken' // 토큰을 저장할 이름
const USER_KEY = 'meokji.user' // 사용자 정보를 저장할 이름

// ----- 토큰 -----

// 저장된 토큰 꺼내기. 없으면 null
export const getToken = () => {
  return localStorage.getItem(TOKEN_KEY)
}

// 토큰 저장하기 (로그인했을 때)
export const saveToken = (token) => {
  localStorage.setItem(TOKEN_KEY, token)
}

// 토큰 지우기 (로그아웃했을 때)
export const removeToken = () => {
  localStorage.removeItem(TOKEN_KEY)
}

// ----- 사용자 정보 -----
// localStorage 에는 글자(문자열)만 저장된다.
// 그래서 객체를 저장할 때는 JSON.stringify 로 글자로 바꾸고,
// 꺼낼 때는 JSON.parse 로 다시 객체로 바꾼다.
//   { nickname: '홍길동' }  --JSON.stringify-->  '{"nickname":"홍길동"}'

// 저장된 사용자 정보 꺼내기. 없으면 null
export const getSavedUser = () => {
  const saved = localStorage.getItem(USER_KEY)

  // 토큰이 없으면 사용자 정보가 있어도 로그인 안 한 것으로 본다
  if (saved === null || getToken() === null) {
    return null
  }

  return JSON.parse(saved)
}

// 사용자 정보 저장하기
export const saveUser = (user) => {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

// 사용자 정보 지우기
export const removeUser = () => {
  localStorage.removeItem(USER_KEY)
}
