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
//
// 왜 useState 가 아닌가
//
// useState 는 새로고침하면 사라진다. 로그인 상태가 그렇게 풀리면
// 화면을 넘길 때마다 다시 로그인해야 한다.
//
// 왜 토큰과 사용자 정보를 둘 다 저장하나
//
// 토큰만 저장하면 새로고침할 때마다 /api/auth/me 를 불러 닉네임을
// 받아와야 하고, 그 사이에 화면 위쪽이 비어 보인다. 사용자 정보를
// 같이 두면 바로 그릴 수 있다.
//
// 다만 그 정보는 옛 값일 수 있다. 토큰이 진짜인지는 서버가 보고,
// 이 정보는 화면에 띄우는 용도일 뿐이다 - 권한을 가리는 데 쓰면 안 된다
// (개발자도구에서 손으로 고칠 수 있다).

// 앞에 'meokji.' 를 붙이는 이유 — localStorage 는 그 주소(도메인)에서
// 돌아가는 모든 코드가 같이 쓴다. 'token' 처럼 흔한 이름으로 두면
// 다른 라이브러리가 덮어쓸 수 있다
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
  // const 가 아니라 let 이다. 아래에서 다시 넣기 때문이다 -
  // 토큰과 달리 두 번에 나눠 찾는 모양이라 값을 바꿔야 한다
  let saved = localStorage.getItem(USER_KEY)

  if (saved === null) {
    saved = sessionStorage.getItem(USER_KEY)
  }

  // 토큰이 없으면 사용자 정보가 있어도 로그인 안 한 것으로 본다
  //
  // 왜 이 확인이 필요한가 — 토큰만 만료되어 지워지고 사용자 정보는
  // 남는 경우가 있다. 그때 닉네임이 보이면 로그인된 것처럼 보이는데,
  // API 를 부르면 전부 401 이 온다
  if (saved === null || getToken() === null) {
    return null
  }

  // 글자를 다시 객체로 바꾼다. 저장된 값이 깨져 있으면 여기서
  // 터지는데, 우리가 넣은 값만 들어 있으니 그럴 일이 없다
  return JSON.parse(saved)
}

// 사용자 정보 저장하기. keep 은 saveToken 과 같은 뜻이다.
//
// 내 정보가 바뀌었을 때(MyPage)도 이 함수를 부르는데, 그때는 keep 을 넘기지
// 않는다. 그래서 '지금 토큰이 들어 있는 쪽' 을 보고 거기에 맞춰 넣는다.
export const saveUser = (user, keep) => {
  const text = JSON.stringify(user)

  // keep 을 안 넘겼으면 토큰이 있는 쪽을 따라간다
  //
  // 따라가지 않으면 토큰과 사용자 정보가 다른 곳에 들어간다.
  // 그러면 탭을 닫았을 때 토큰만 사라지고 정보는 남아, 위
  // getSavedUser 의 확인에 걸려 로그인이 조용히 풀린 것처럼 보인다
  //
  // keep === undefined 로 가리는 이유 — false 와 구별해야 한다.
  // if (!keep) 으로 적으면 "유지 안 함" 을 "안 넘김" 으로 읽는다
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
