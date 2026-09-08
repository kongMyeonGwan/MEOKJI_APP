/**
 * 로그인 토큰을 브라우저에 저장하고 꺼내는 함수 세 개.
 *
 * 토큰을 왜 localStorage 에 두나
 * -----------------------------
 * 새로고침하거나 탭을 다시 열어도 로그인이 유지되어야 하는데,
 * 리액트의 useState 는 새로고침하면 전부 사라진다.
 * localStorage 는 브라우저를 꺼도 남아 있어서 여기에 둔다.
 *
 * 단점도 있다. 페이지에 악성 스크립트가 끼어들면 읽어갈 수 있다(XSS).
 * 더 안전한 방법은 HttpOnly 쿠키인데 그건 CSRF 대응을 따로 해야 해서
 * 이번 프로젝트 범위를 넘는다. 대신 토큰 수명을 30분으로 짧게 잡았다.
 *
 * 이 파일을 따로 뺀 이유
 * ----------------------
 * api/client.js 와 contexts/AuthContext.jsx 가 같은 토큰을 봐야 하는데,
 * 서로를 import 하면 "네가 먼저" 하고 서로를 기다리는 순환 참조가 된다.
 * 둘 다 이 파일만 바라보게 하면 그런 일이 없다.
 */

// localStorage 에 저장될 이름표. 다른 사이트 값과 안 섞이게 앞에 meokji. 를 붙였다.
const TOKEN_KEY = 'meokji.accessToken'

/** 저장된 토큰을 꺼낸다. 없으면 null. */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

/** 로그인에 성공했을 때 토큰을 저장한다. */
export function saveToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

/** 로그아웃하거나 토큰이 만료됐을 때 지운다. */
export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}
