# 인증(회원가입 · 로그인 · 비밀번호 재설정) 학습 가이드

이 문서는 **직접 손을 움직여서 익히는** 쪽이다. 깨뜨려보고, 질문에 말로 답해보고,
마지막에 백지에서 다시 써본다.

흐름을 순서대로 읽고 싶으면 [STUDY_JWT_FLOW.md](STUDY_JWT_FLOW.md) 를 먼저 보는 것이
낫다. 거기는 "로그인 버튼부터 토큰 만료까지" 를 코드와 함께 따라가는 문서다.

> 2026-10-06 에 지금 코드 기준으로 전부 다시 썼다. 그 전 버전은
> `LoginUserArgumentResolver` · `JwtProvider` · `@LoginUser` · `SecurityConfig` 를
> 설명하고 있었는데 **지금 저장소에 그런 파일이 하나도 없다.** 옛 구조를 설명한
> 문서라서 통째로 갈아엎었다.

---

## 0. 먼저 — 인증은 어디에 흩어져 있나

### 백엔드

```
auth/
├── controller/AuthController.java        6개 엔드포인트
├── service/AuthService.java              인터페이스
├── service/AuthServiceImpl.java          회원가입 · 로그인 · 토큰 해석  ★ 핵심
├── service/PasswordResetService(Impl)    비밀번호 재설정 3단계
├── dao/VerificationCodeDAO.java          인증번호 표
└── vo/                                   LoginVO · RegisterVO · TokenVO ...

util/
├── JwtUtil.java                          토큰 만들기 · 읽기            ★ 핵심
└── Passwords.java                        비밀번호 길이 검사

config/WebConfig.java                     BCryptPasswordEncoder 빈 + CORS

user/
├── vo/UserVO.java                        isAdmin() · isLockedAccount()
└── dao/UserDAO.java                      findActiveByEmail · lockAccount ...
```

### 프론트엔드

```
pages/auth/
├── LoginPage.jsx             로그인 폼 + 소개 + 게임 배너
├── RegisterPage.jsx          회원가입 폼
├── PasswordResetPage.jsx     재설정 3단계
└── components/CodeInput.jsx  인증번호 6칸 입력

api/auth.js                   6개 함수
utils/token.js                토큰 보관 · 꺼내기 · authHeader()   ★ 핵심
App.jsx                       user state 를 들고 props 로 내려줌
```

**`AuthServiceImpl` · `JwtUtil` · `token.js` 셋만 제대로 읽으면 90% 다.**

---

## 1단계. 흐름을 끝까지 따라가기

### 회원가입을 누르면

```
1  RegisterPage.jsx            폼 검사 (빈 칸 · 비밀번호 확인)
2  api/auth.js register()      POST /api/auth/register
3  AuthController.register()   @Valid 가 형식을 검사 → 틀리면 422
4  AuthServiceImpl.register()  ① Passwords.checkBytes  비밀번호 72바이트 넘는지
                               ② findActiveByEmailOrNickname  중복 확인 → 409
                               ③ passwordEncoder.encode()  해시로 바꿔 저장
                               ④ RegionCatalog 로 주소 → 좌표 변환
5  userDAO.insert()            USERS 에 한 행
6  LoginPage 로 이동            가입하면 바로 로그인시키지 않는다
```

**72바이트 제한이 왜 있나?** BCrypt 는 입력의 앞 72바이트만 본다. 더 길면 조용히
잘려서, 73자째부터 아무 글자나 넣어도 로그인이 된다. 그래서 미리 막는다.
한글은 한 글자가 3바이트라 24자쯤에서 걸린다.

### 로그인 버튼을 누르면

```
1  LoginPage.jsx               빈 칸 확인
2  api/auth.js login()         POST /api/auth/login
3  AuthServiceImpl.login()     ① findActiveByEmail       없으면 401
                               ② isLockedAccount()       잠겼으면 423
                               ③ checkPassword()         틀리면 loginFailed()
                               ④ resetLoginFailCount()   성공하면 0 으로
4  AuthController.login()      jwtUtil.createToken(user.getId())
5  TokenVO 로 응답              { access_token, token_type, expires_in, user }
6  App.jsx handleLogin()       saveToken / saveUser / setUser
7  navigate('/')               홈으로
```

**토큰을 만드는 곳이 서비스가 아니라 컨트롤러다.** 서비스는 "이 사람이 맞는가"
까지만 책임지고, 토큰 발급은 컨트롤러가 한다. `register` 는 토큰을 안 만드는데
`login` 만 만드는 차이를 거기서 읽을 수 있다.

### 보호된 API를 부르면

```
1  utils/token.js authHeader()   Authorization: Bearer xxx  (없으면 빈 객체)
2  api/*.js                      headers: authHeader() 로 붙여서 보냄
3  XxxController                 @RequestHeader("Authorization") String token
4  AuthServiceImpl.getLoginUser  ① "Bearer " 떼기
                                 ② JwtUtil.getUserId()  서명 · 만료 확인
                                 ③ userDAO.findById()   탈퇴 확인
5  XxxService                    UserVO 를 받아서 처리
```

**필터가 없다.** 스프링 시큐리티를 안 쓰기 때문에, 컨트롤러마다 헤더를 직접 받아
서비스에 넘긴다. 확인 함수는 셋이다.

| 함수 | 토큰이 없거나 틀리면 | 예 |
|---|---|---|
| `getLoginUser(token)` | 401 던짐 | `/api/likes`, `/api/histories` |
| `findLoginUser(token)` | `null` 돌려줌 | `/api/recommendations` |
| `getAdminUser(token)` | 관리자 아니면 403 | `POST /api/notices` |

### 새로고침했는데 로그인이 유지되는 이유

```
1  App.jsx                    useState(getSavedUser())  로 시작한다
2  utils/token.js             localStorage / sessionStorage 에서 꺼냄
3  App.jsx useEffect          getMe() 로 "이 토큰 아직 살아있나?" 확인
4                             200 이면 최신 정보로 갱신 / 401 이면 handleLogout()
```

**화면이 먼저 뜨고 확인은 나중에 온다.** `getSavedUser()` 가 동기라서 첫 그림부터
로그인 상태로 보이고, 서버 확인 결과가 돌아오면 그때 바로잡는다. 그래서 토큰이
만료됐어도 아주 잠깐은 로그인한 것처럼 보인다.

### 비밀번호 재설정 3단계

```
1  send(email)      인증번호 6자리 생성 → VERIFICATION_CODES 저장 → 메일 발송
                    ResetCodeMailer. app.mail.provider=mock 이면 콘솔에 찍는다
2  verify(email, code)   맞는지 확인. 틀리면 시도 횟수 +1 (5회까지)
3  change(email, code, newPassword)   해시로 바꿔 저장 + 계정 잠금 해제
```

메일 서버가 없어도 끝까지 테스트할 수 있다. `provider=mock` 이면 인증번호가
**백엔드 콘솔에 찍힌다.** 그걸 복사해서 화면에 넣으면 된다.

---

## 2단계. 일부러 깨뜨려보기 (가장 중요)

읽는 것보다 이게 훨씬 오래 남는다. **하나씩 해보고 반드시 되돌릴 것.**

### 실험 1 — 토큰 내용을 직접 읽어본다

로그인한 뒤 브라우저 F12 콘솔에서.

```js
const t = localStorage.getItem('meokji.accessToken')
JSON.parse(atob(t.split('.')[1]))
// → { sub: "282", iat: 1761234567, exp: 1761236367 }
```

- **핵심**: JWT 는 암호화가 아니라 **Base64 인코딩**이다. 누구나 읽을 수 있다
- 그래서 토큰에 비밀번호나 개인정보를 넣으면 안 된다
- 이 프로젝트가 회원 번호와 시각만 넣는 이유가 이것이다

### 실험 2 — 토큰을 한 글자 고친다

```js
const t = localStorage.getItem('meokji.accessToken')
localStorage.setItem('meokji.accessToken', t.slice(0, -1) + 'X')
// 새로고침
```

- 예상: 어떻게 될까? 그냥 통과할까, 401 이 날까?
- **핵심**: 마지막 토막이 **서명**이다. 한 글자만 달라져도 `JwtUtil.getUserId()` 가
  예외를 잡아 `null` 을 돌려주고, `getLoginUser` 가 401 을 던진다
- 내용(`sub`)을 `"1"` 로 바꿔도 마찬가지다. 서명을 다시 만들려면 비밀키가 필요하다
- 되돌리려면 다시 로그인

### 실험 3 — JWT 비밀키를 바꾼다

`backend/src/main/resources/application-local.yml` 의 `app.jwt.secret-key` 를 아무
글자로 바꾸고 서버 재시작.

- 예상: 이미 로그인한 사람은 어떻게 될까?
- **핵심**: **전원이 즉시 로그아웃된다.** 서버가 토큰을 저장하지 않고 서명만 보기
  때문에, 키가 바뀌면 전에 발급한 토큰이 전부 "위조" 로 판정된다
- 운영 중에 키를 바꾸면 안 되는 이유다. 거꾸로, 키가 유출됐을 때 쓰는 유일한 수단이기도 하다
- 32글자보다 짧게 넣으면 **서버가 아예 안 뜬다.** HS256 이 요구하는 길이다

### 실험 4 — 만료 시간을 1분으로 줄인다

```yaml
app:
  jwt:
    expire-minutes: 1
```

재시작하고 로그인한 뒤 1분을 기다린다.

- 예상: 화면이 자동으로 로그아웃될까?
- **핵심**: **안 된다.** 켜둔 화면은 그대로 있고, 다음 API 호출에서 401 이 나며
  에러 문구만 뜬다. `App.jsx` 의 자동 로그아웃은 **앱이 켜질 때 한 번만** 돈다
- 실무에서는 axios 인터셉터로 모든 401 을 가로채 로그아웃시킨다. 이 프로젝트는
  인터셉터를 안 쓴다 — 왜 그렇게 했을지, 붙인다면 어디에 붙일지 생각해볼 것

### 실험 5 — `authHeader()` 를 빼먹는다

`frontend/src/api/likes.js` 의 `getLikes` 에서 `{ headers: authHeader() }` 를 지운다.

```js
const response = await client.get('/api/likes')   // headers 를 지움
```

- 예상: 에러가 날까, 빈 목록이 올까?
- **핵심**: `/api/likes` 는 `getLoginUser` 를 쓰므로 **401** 이 난다. 찜 목록이 통째로
  안 뜬다. 인터셉터가 없어서 **함수마다 직접 붙여야 하고, 하나라도 빠뜨리면 그
  요청만 비회원으로 나간다**
- 새 API 함수를 만들 때 가장 많이 하는 실수다

### 실험 6 — `getLoginUser` 를 `findLoginUser` 로 바꾼다

`LikeController.getList` 에서 `authService.getLoginUser(token)` 을
`findLoginUser(token)` 으로 바꾸고 재시작. 로그인하지 않은 채로 `/api/likes` 호출.

- 예상: 401 이 날까?
- **핵심**: **401 이 안 난다.** `loginUser` 가 `null` 로 서비스까지 내려가서
  `NullPointerException` 이나 엉뚱한 결과가 나온다. 둘의 차이가 "로그인 필수인가
  아닌가" 를 정하는 유일한 장치다
- 이게 **스프링 시큐리티를 안 쓸 때의 가장 큰 위험**이다. 새 컨트롤러에서 확인
  코드를 아예 빼먹으면 그 API 는 **에러도 없이 그냥 뚫린다**

### 실험 7 — 잠금 검사를 지운다

`AuthServiceImpl.login()` 에서 `isLockedAccount()` 블록을 주석 처리하고, 일부러
비밀번호를 20번 틀려서 잠긴 계정으로 로그인해본다.

- **핵심**: 잠겨도 로그인이 된다. 잠금은 DB 의 `IS_LOCKED` 칸일 뿐이고, **그걸 보는
  코드가 없으면 아무 의미가 없다.** 보안 장치는 "데이터" 가 아니라 "검사" 다
- 되돌릴 것. 그리고 잠긴 계정은 비밀번호 재설정으로 풀린다

### 실험 8 — 에러 문구를 다르게 만든다

`login()` 에서 "없는 이메일" 과 "틀린 비밀번호" 의 문구를 다르게 바꿔본다.

- **핵심**: 지금은 둘 다 `LOGIN_FAIL` 로 같다. 다르게 하면 공격자가 **"이 이메일은
  가입돼 있구나" 를 알아낼 수 있다**(계정 열거 공격). 일부러 같게 둔 것이다
- 쓰기 편한 UX 와 보안이 부딪히는 대표적인 자리다

### 실험 9 — 비밀번호 해시를 눈으로 본다

DBeaver 나 sqlplus 에서.

```sql
SELECT email, password_hash FROM users WHERE use_yn = 'Y';
```

```
$2a$10$N9qo8uLOickgx2ZMRZoMye...
 │   │  └ 솔트 + 해시
 │   └ 반복 횟수 (2^10 = 1024번)
 └ BCrypt 라는 표시
```

- **핵심**: 같은 비밀번호로 두 계정을 만들어도 **해시가 다르다.** 솔트가 매번
  달라서다. 그래서 `=` 로 비교할 수 없고 반드시 `matches()` 를 써야 한다
- 해시에서 원래 비밀번호를 되돌릴 방법은 없다. 그래서 "비밀번호 찾기" 가 아니라
  "비밀번호 재설정" 인 것이다

### 실험 10 — 로그인 상태 유지를 꺼본다

체크를 풀고 로그인한 뒤, F12 → Application → Storage 에서 확인한다.

- `sessionStorage` 에 `meokji.accessToken` 이 있고 `localStorage` 에는 없어야 한다
- 탭을 닫았다 다시 열면 로그아웃돼 있어야 한다
- **핵심**: `saveToken` 에서 반대쪽을 지우는 줄을 지워보면, "유지 안함" 으로 다시
  로그인해도 계속 유지된다. `getToken()` 이 `localStorage` 를 먼저 보기 때문이다

---

## 3단계. 말로 답해보기

막히는 게 있으면 1·2단계로 돌아간다.

### JWT 자체

1. JWT 세 토막은 각각 무엇인가? 점으로 나뉘는 이유는?
2. 토큰 내용을 아무나 읽을 수 있는데 왜 안전하다고 하나?
3. 서명은 무엇으로 만드나? 서명이 막아주는 것과 못 막아주는 것은?
4. 서버는 발급한 토큰을 어디에 저장하나?
5. 비밀키가 유출되면 무슨 일이 일어나나?

### 이 프로젝트의 선택

6. 왜 세션이 아니라 JWT 인가? 서버를 여러 대로 늘릴 때 무엇이 달라지나?
7. 서명이 맞는데도 `userDAO.findById()` 를 또 부르는 이유는?
8. `getLoginUser` 와 `findLoginUser` 의 차이는? 어느 API 가 어느 쪽을 쓰나?
9. 로그아웃이 서버에 아무 요청도 안 보내는데, 그래도 되나? 무엇이 문제인가?
10. 만료를 30분으로 둔 이유는? 1시간이면 무엇이 나빠지나? 5분이면?

### 스프링

11. 스프링 시큐리티를 안 쓰는데 `spring-security-crypto` 는 왜 들어 있나?
12. 토큰 발급이 서비스가 아니라 컨트롤러에 있는 이유는?
13. `AuthService` 를 인터페이스와 `Impl` 로 나눈 이유는?
14. `@Valid` 가 걸러주는 것과 서비스가 걸러야 하는 것의 경계는?

### 프론트엔드

15. 토큰을 `localStorage` 와 `sessionStorage` 중 어디에 두는가? 무엇이 정하나?
16. `authHeader()` 가 빈 객체를 돌려줄 때는 언제이고, 그때 서버는 어떻게 되나?
17. 새로고침해도 로그인이 유지되는 원리를 두 단계로 설명해보라
18. `App.jsx` 가 `user` 를 들고 props 로 내려주는데, Context 를 안 쓴 이유는?

### 비밀번호

19. 비밀번호를 72바이트로 제한하는 이유는?
20. 같은 비밀번호인데 해시가 다른 이유는?
21. "없는 이메일" 과 "틀린 비밀번호" 의 문구를 같게 둔 이유는?
22. 계정 잠금은 어디에 저장되고, 어떻게 풀리나?

---

## 4단계. 백지에서 다시 써보기

진짜로 이해했는지는 이걸로 판가름난다.

### 규칙

- 기존 코드를 **보지 않고** 쓴다. 막히면 2분만 고민하고 열어본다
- 완벽하지 않아도 된다. **흐름이 이어지는지**가 핵심이다
- 새 폴더(`study/`)에 쓰고, 다 되면 지운다

### 순서

**백엔드**

1. `JwtUtil` — `createToken(Long)` 과 `getUserId(String)` 둘만
2. `UserVO` — 토큰에 필요한 칸만 (`id`, `email`, `passwordHash`, `useYn`)
3. `AuthServiceImpl.login()` — 회원 찾기 → 비밀번호 비교 → 돌려주기
4. `AuthServiceImpl.getLoginUser()` — "Bearer " 떼기 → 토큰 풀기 → DB 조회
5. `AuthController` — `/login` 과 `/me` 둘만
6. 보호된 API 하나 — 아무거나 하나 골라 `getLoginUser` 를 붙여본다

**프론트엔드**

7. `token.js` — `getToken` · `saveToken` · `removeToken` · `authHeader`
8. 로그인 폼 — `useState` 두 개와 `handleSubmit` 하나
9. `App.jsx` — `user` state + `handleLogin` + `handleLogout`

### 다 만든 뒤

- 로그인 → 토큰이 저장되나?
- 새로고침 → 유지되나?
- 토큰을 고치면 → 401 이 나나?
- 로그아웃 → 지워지나?

넷이 다 되면 인증은 이해한 것이다.

---

## 5단계. 더 해볼 것 (여유가 있으면)

### 쉬움

- `expires_in` 을 화면에서 써보기. "n분 뒤 자동 로그아웃됩니다" 를 띄운다
- 관리자 계정으로 로그인해서 `user_level` 이 응답에 안 나가는지 확인해보기
  (`UserVO` 의 `@JsonIgnore` 가 어디에 붙어 있는지 찾아볼 것)

### 보통

- axios 인터셉터를 붙여서 401 이 오면 자동 로그아웃시키기.
  `api/client.js` 에 `client.interceptors.response.use(...)` 를 추가한다
- `authHeader()` 를 매번 쓰는 대신 요청 인터셉터로 자동으로 붙이기.
  **그러면 비회원도 되는 API 에 어떤 영향이 있는지** 같이 생각해볼 것

### 어려움

- refresh token 을 설계해보기. access 는 5분, refresh 는 2주로 두면
  어디에 저장하고 어떻게 갱신할 것인가?
- 스프링 시큐리티로 바꿔보기. 별도 브랜치에서.
  `SecurityFilterChain` + `OncePerRequestFilter` 로 JWT 를 처리하고,
  **CORS 설정을 `CorsConfigurationSource` 빈으로 옮기는 것**을 잊지 말 것
  (안 옮기면 프리플라이트가 401 로 막혀 프론트 전체가 죽는다)

---

## 부록. 파일 한 줄 요약

| 파일 | 역할 |
|---|---|
| `AuthController` | 회원가입 · 로그인 · 내 정보 · 비밀번호 재설정 3단계 (6개) |
| `AuthServiceImpl` | 비밀번호 확인 · 계정 잠금 · 토큰 해석 (`getLoginUser` 등 3개) |
| `PasswordResetServiceImpl` | 인증번호 발송 · 확인 · 비밀번호 변경 |
| `JwtUtil` | 토큰 만들기 · 읽기. **JWT 라이브러리를 쓰는 유일한 파일** |
| `Passwords` | 비밀번호 72바이트 검사 |
| `UserVO` | `isAdmin()` · `isLockedAccount()` 판단 + 응답에서 뺄 칸 표시 |
| `WebConfig` | `BCryptPasswordEncoder` 빈 + CORS |
| `token.js` | 토큰 보관 위치 결정 · `authHeader()` |
| `App.jsx` | `user` state 를 들고 props 로 내려줌 |
| `LoginPage.jsx` | 로그인 폼 + 로그인 상태 유지 체크 |

### 계층을 나눈 이유

```
Controller   HTTP 를 아는 유일한 층. 헤더 · 상태코드 · @Valid
     ↓
Service      규칙이 사는 곳. "비밀번호가 틀리면 횟수를 올린다" 같은 것
     ↓
DAO          SQL 만. 매퍼 XML 과 한 쌍
```

이렇게 나누면 **테스트가 쉬워진다.** 서비스는 HTTP 를 모르므로 객체만 넣어
부를 수 있다. `src/test/java/.../AuthFixtureTest.java` 에 로그인 · 내 정보 ·
잘못된 토큰 · 중복 이메일 테스트가 들어 있으니 같이 읽어볼 것.

### 관련 문서

- [STUDY_JWT_FLOW.md](STUDY_JWT_FLOW.md) — 로그인부터 만료까지 순서대로 읽는 문서
- [STUDY_COLUMN_TYPES.md](STUDY_COLUMN_TYPES.md) — 컬럼 타입을 왜 그렇게 골랐나
- [BOARD_SPEC.md](BOARD_SPEC.md) — 게시판 · 공지 명세 (담당자용)
