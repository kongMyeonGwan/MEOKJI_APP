// 로그인 정보를 브라우저에 저장하고 꺼내는 함수들.
//
// 어디에 저장하나? 로그인 화면의 '로그인 상태 유지' 체크에 따라 둘 중 하나다.
//   체크함   localStorage   브라우저를 껐다 켜도 남는다
//   체크 안함 sessionStorage 탭을 닫으면 사라진다 (공용 PC 용)
//
// 둘 다 useState 와 달리 새로고침해도 남는다는 점은 같다.
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
// 어디에 넣었는지는 모르니 둘 다 본다
export const getToken = () => {
  const saved = localStorage.getItem(TOKEN_KEY)

  if (saved !== null) {
    return saved
  }

  return sessionStorage.getItem(TOKEN_KEY)
}

// 토큰 저장하기 (로그인했을 때).
// keep 이 true 면 localStorage, false 면 sessionStorage.
// 반대쪽은 지운다 - 안 지우면 '유지 안함' 으로 다시 로그인해도 예전 토큰이 남아
// getToken 이 그걸 먼저 집어온다.
export const saveToken = (token, keep) => {
  if (keep) {
    localStorage.setItem(TOKEN_KEY, token)
    sessionStorage.removeItem(TOKEN_KEY)
  } else {
    sessionStorage.setItem(TOKEN_KEY, token)
    localStorage.removeItem(TOKEN_KEY)
  }
}

// 토큰 지우기 (로그아웃했을 때). 어디에 있든 지운다
export const removeToken = () => {
  localStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(TOKEN_KEY)
}

// 로그인이 필요한 API 를 부를 때 쓰는 헤더.
//
// 쓰는 법: await client.get('/api/bans', { headers: authHeader() })
// 모양:    Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
//
// 로그인 안 했으면 빈 객체를 돌려준다. 그래야 로그인 없이도 되는 API
// (추천 · 레시피 · 게시판 조회)를 게스트가 부를 때 문제가 없다.
export const authHeader = () => {
  const token = getToken()

  if (!token) {
    return {}
  }

  return { Authorization: `Bearer ${token}` }
}

// ----- 사용자 정보 -----
// localStorage 에는 글자(문자열)만 저장된다.
// 그래서 객체를 저장할 때는 JSON.stringify 로 글자로 바꾸고,
// 꺼낼 때는 JSON.parse 로 다시 객체로 바꾼다.
//   { nickname: '홍길동' }  --JSON.stringify-->  '{"nickname":"홍길동"}'

// 저장된 사용자 정보 꺼내기. 없으면 null
export const getSavedUser = () => {
  let saved = localStorage.getItem(USER_KEY)

  if (saved === null) {
    saved = sessionStorage.getItem(USER_KEY)
  }

  // 토큰이 없으면 사용자 정보가 있어도 로그인 안 한 것으로 본다
  if (saved === null || getToken() === null) {
    return null
  }

  return JSON.parse(saved)
}

// 사용자 정보 저장하기. keep 은 saveToken 과 같은 뜻이다.
//
// 내 정보가 바뀌었을 때(MyPage)도 이 함수를 부르는데, 그때는 keep 을 넘기지
// 않는다. 그래서 '지금 토큰이 들어 있는 쪽' 을 보고 거기에 맞춰 넣는다.
export const saveUser = (user, keep) => {
  const text = JSON.stringify(user)

  // keep 을 안 넘겼으면 토큰이 있는 쪽을 따라간다
  let toLocal = keep
  if (keep === undefined) {
    toLocal = localStorage.getItem(TOKEN_KEY) !== null
  }

  if (toLocal) {
    localStorage.setItem(USER_KEY, text)
    sessionStorage.removeItem(USER_KEY)
  } else {
    sessionStorage.setItem(USER_KEY, text)
    localStorage.removeItem(USER_KEY)
  }
}

// 사용자 정보 지우기. 어디에 있든 지운다
export const removeUser = () => {
  localStorage.removeItem(USER_KEY)
  sessionStorage.removeItem(USER_KEY)
}
