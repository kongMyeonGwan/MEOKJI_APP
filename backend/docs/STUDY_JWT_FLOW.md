# JWT 로그인, 처음부터 끝까지

이 프로젝트에서 **로그인 버튼을 누른 순간부터 토큰이 만료되기까지** 무슨 일이
일어나는지를 코드와 함께 따라가는 문서다. 밖에서 읽을 수 있게 코드 조각을 그대로
옮겨 담았으므로, 에디터를 안 열어도 끝까지 읽힌다.

> [STUDY_AUTH.md](STUDY_AUTH.md) 와 한 쌍이다. 그쪽은 **직접 깨뜨려보는 실험** 중심이라
> PC 앞에서 봐야 하고, 이 문서는 **순서대로 읽기만 하면 되는** 쪽이라 이동 중에 읽기 좋다.

---

## 0. 한눈에

```
[로그인]
  이메일 · 비밀번호
        ↓  POST /api/auth/login
  BCrypt 로 비밀번호 비교
        ↓
  JwtUtil.createToken(282)
        ↓
  eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyODIi...   ← 회원번호 + 만료시각 + 서명
        ↓
  localStorage 또는 sessionStorage 에 보관

[그 뒤 모든 요청]
  Authorization: Bearer eyJhbGci...
        ↓
  JwtUtil.getUserId()   ① 서명 확인  ② 만료 확인
        ↓
  userDAO.findById(282) ③ 탈퇴 확인
        ↓
  UserVO → 서비스로 전달

[30분 뒤]
  모든 요청이 401 → App.jsx 가 자동 로그아웃
```

핵심 파일은 다섯이다.

| 파일 | 하는 일 |
|---|---|
| `frontend/src/pages/auth/LoginPage.jsx` | 로그인 폼 |
| `frontend/src/utils/token.js` | 토큰 보관 · 꺼내기 · 헤더 만들기 |
| `backend/.../auth/controller/AuthController.java` | 로그인 요청을 받는 자리 |
| `backend/.../auth/service/AuthServiceImpl.java` | 비밀번호 확인 · 토큰 해석 |
| `backend/.../util/JwtUtil.java` | 토큰 만들기 · 읽기 (JWT 자체는 여기뿐) |

---

## 0-1. 먼저 — 이름 셋을 구분한다

여기서 가장 많이 헷갈린다. **생김새가 비슷하고 이름에 전부 token / key 가 들어가는데
서로 완전히 다른 것**이다.

| | 누가 정하나 | 무엇인가 | 어디에 있나 |
|---|---|---|---|
| `TOKEN_KEY` | **프론트** | 브라우저 저장소에서 쓸 **이름표** | `utils/token.js` 에 글자로 박혀 있음 |
| `accessToken` | **서버** | API 인증에 쓰는 **실제 JWT** | 브라우저 저장소 + 요청 헤더 |
| `secret-key` | **서버** | 서명을 만들고 검증하는 **비밀키** | `application.yml`. 서버 밖으로 절대 안 나감 |

코드로 보면 이렇다.

```js
const TOKEN_KEY = 'meokji.accessToken'   // ← 이름표. 프론트가 정한 글자
localStorage.setItem(TOKEN_KEY, token)   // ← 값. 서버가 준 JWT
//                   └ 이름        └ 값
```

```
localStorage
┌────────────────────────┬──────────────────────────────────┐
│ meokji.accessToken     │ eyJhbGciOiJIUzI1NiJ9.eyJzdWIi... │
│   ↑ TOKEN_KEY          │   ↑ accessToken                  │
└────────────────────────┴──────────────────────────────────┘
```

**확인 질문**

- `TOKEN_KEY` 도 서버가 주나? → **아니다.** 프론트가 정한 이름이다.
  `'meokji.myToken'` 으로 바꿔도 서버는 아무 영향을 안 받는다
- 서버에 보내는 것은 이름인가 값인가? → **값**이다.
  `Authorization: Bearer eyJhbGci...`
- `secret-key` 를 프론트로 보내야 하나? → **절대 안 된다.**
  그게 새면 누구나 "회원 1번" 짜리 토큰을 만들 수 있다

---

## 1단계 — 로그인 버튼을 누른다

`frontend/src/pages/auth/LoginPage.jsx`

```jsx
const result = await login({ email: values.email.trim(), password: values.password })
```

`api/auth.js` 가 `POST /api/auth/login` 으로 보낸다.

**여기까지는 JWT 와 아무 상관이 없다.** 이메일과 비밀번호를 평범하게 보내는
요청이다. 비밀번호가 평문으로 나가는 것이 걱정된다면, 그건 JWT 가 아니라
HTTPS 가 해결할 문제다 (배포할 때 https 를 쓰면 통째로 암호화된다).

보내기 전에 화면에서 빈 칸만 확인한다. 진짜 검사는 서버가 한다.

```jsx
if (values.email.trim() === '') {
  setError('이메일을 입력해주세요')
  emailRef.current.focus()
  return
}
```

---

## 2단계 — 서버가 신원을 확인한다

`backend/.../auth/service/AuthServiceImpl.java` 의 `login()`

```java
// 1. 이메일로 회원 찾기
UserVO user = userDAO.findActiveByEmail(request.getEmail());
if (user == null) {
    throw ApiException.unauthorized(LOGIN_FAIL);
}

// 2. 잠긴 계정이면 비밀번호 확인도 안 한다
if (user.isLockedAccount()) {
    throw ApiException.locked("계정이 잠겼습니다. 비밀번호를 재설정해주세요.");
}

// 3. 비밀번호 확인
if (!checkPassword(request.getPassword(), user.getPasswordHash())) {
    loginFailed(user); // 여기서 항상 에러를 던진다
}

// 4. 로그인 성공. 그동안 틀린 횟수를 0 으로 되돌린다
if (user.getLoginFailCount() != null && user.getLoginFailCount() > 0) {
    userDAO.resetLoginFailCount(user.getId());
    user.setLoginFailCount(0);
}
```

### 비밀번호는 어떻게 비교하나

DB 의 `USERS.PASSWORD_HASH` 에는 **해시만** 들어 있다. 원래 비밀번호는 어디에도
없고, 꺼내 볼 수도 없다.

```
$2a$10$N9qo8uLOickgx2ZMRZoMye...
 │   │  └ 솔트 + 해시
 │   └ 반복 횟수 (2^10 = 1024번)
 └ BCrypt 라는 표시
```

`BCryptPasswordEncoder.matches(입력한_비밀번호, 저장된_해시)` 가 "입력한 것을 같은
솔트로 같은 횟수만큼 해시하면 저장된 것과 같은가" 를 본다. 같은 비밀번호라도
솔트가 달라서 **가입할 때마다 해시 글자가 다르다.** 그래서 `=` 로 비교할 수 없고
반드시 `matches()` 를 써야 한다.

### 틀렸을 때 — 계정 잠금

```java
private void loginFailed(UserVO user) {
    int failCount = 1;
    if (user.getLoginFailCount() != null) {
        failCount = user.getLoginFailCount() + 1;
    }

    if (failCount >= maxLoginFailCount) {   // 기본 20회
        userDAO.lockAccount(user.getId());
        throw ApiException.locked("로그인 " + maxLoginFailCount + "회 실패로 계정이 잠겼습니다...");
    }

    userDAO.increaseLoginFailCount(user.getId());
    ...
}
```

**"없는 이메일" 과 "틀린 비밀번호" 의 에러 문구가 같다** (`LOGIN_FAIL`). 문구가
다르면 공격자가 "이 이메일은 가입돼 있구나" 를 알아낼 수 있다. 일부러 똑같이 둔 것이다.

---

## 3단계 — 토큰을 만든다 (JWT 가 처음 나오는 곳)

`backend/.../auth/controller/AuthController.java`

```java
@PostMapping("/login")
public TokenVO login(@Valid @RequestBody LoginVO request) {
    UserVO user = authService.login(request);

    TokenVO response = new TokenVO();
    response.setAccessToken(jwtUtil.createToken(user.getId()));
    response.setTokenType("bearer");
    response.setExpiresIn(jwtUtil.getExpiresInSeconds());
    response.setUser(user);
    return response;
}
```

`backend/.../util/JwtUtil.java`

```java
public String createToken(Long userId) {
    Date now = new Date();
    Date expireAt = new Date(now.getTime() + getExpiresInSeconds() * 1000);

    return Jwts.builder()
            .subject(String.valueOf(userId)) // 회원 번호
            .issuedAt(now)                   // 만든 시각
            .expiration(expireAt)            // 만료 시각
            .signWith(getKey())              // 비밀키로 서명
            .compact();
}

private SecretKey getKey() {
    return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
}
```

### 나온 글자의 생김새

점 두 개로 세 토막이 난다.

```
eyJhbGciOiJIUzI1NiJ9 . eyJzdWIiOiIyODIiLCJpYXQiOjE3NjE... . 4Xf9_K2mQ8vL...
   ①헤더                     ②내용(payload)                      ③서명
```

**① 헤더** — 어떤 알고리즘으로 서명했는지. 풀면 `{"alg":"HS256"}`

**② 내용** — 이 프로젝트는 셋만 넣는다.

```json
{
  "sub": "282",          // 회원 번호 (subject)
  "iat": 1761234567,     // 만든 시각 (issued at)
  "exp": 1761236367      // 만료 시각 (expiration) — 30분 뒤
}
```

**③ 서명** — ①과 ②를 비밀키로 계산한 값.

### 반드시 알아야 할 것: 앞 두 토막은 암호화가 아니다

Base64 라는 **인코딩**일 뿐이다. 누구나 풀어서 읽을 수 있다. 브라우저 콘솔에서
이렇게 해보면 바로 보인다.

```js
const token = localStorage.getItem('meokji.accessToken')
JSON.parse(atob(token.split('.')[1]))
// → { sub: "282", iat: ..., exp: ... }
```

그래서 **토큰에 비밀번호나 주민번호 같은 걸 넣으면 절대 안 된다.** 이 프로젝트가
회원 번호와 시각만 넣는 이유가 이것이다.

### 그럼 안전장치는 무엇인가 — 서명

내용을 `{"sub": "1"}` (관리자인 척) 로 고치면 어떻게 될까? 고칠 수는 있다. 하지만
서명은 **비밀키를 알아야** 다시 계산할 수 있다. 서버는 받은 토큰의 ①②로 서명을
다시 계산해서 ③과 비교하므로, 내용을 건드리면 바로 걸린다.

즉 JWT 는 **"내용을 감추는" 기술이 아니라 "위조를 막는" 기술**이다.

> `Keys.hmacShaKeyFor` 는 HS256 을 쓰는데, 키가 **32바이트(영문 32글자) 이상**이어야
> 한다. 짧으면 서버가 뜰 때 예외가 난다. `application.yml` 의 `app.jwt.secret-key` 다.

---

## 4단계 — 브라우저가 보관한다

`frontend/src/App.jsx`

```jsx
const handleLogin = (token, loginUser, keep) => {
  saveToken(token, keep)    // 브라우저에 토큰 저장
  saveUser(loginUser, keep) // 브라우저에 내 정보 저장
  setUser(loginUser)        // 화면에 반영 (상단바에 닉네임이 뜬다)
}
```

`frontend/src/utils/token.js`

```js
export const saveToken = (token, keep) => {
  if (keep) {
    localStorage.setItem(TOKEN_KEY, token)
    sessionStorage.removeItem(TOKEN_KEY)
  } else {
    sessionStorage.setItem(TOKEN_KEY, token)
    localStorage.removeItem(TOKEN_KEY)
  }
}
```

`keep` 은 로그인 화면의 **"로그인 상태 유지"** 체크다.

| 체크 | 저장 위치 | 언제까지 |
|---|---|---|
| 함 | `localStorage` | 브라우저를 껐다 켜도 남는다 |
| 안 함 | `sessionStorage` | 탭을 닫으면 사라진다 (공용 PC 용) |

**반대쪽을 지우는 줄이 중요하다.** 안 지우면 "유지 안함" 으로 다시 로그인해도
`localStorage` 에 옛 토큰이 남아서, 꺼낼 때 그걸 먼저 집어온다.

```js
export const getToken = () => {
  const saved = localStorage.getItem(TOKEN_KEY)
  if (saved !== null) {
    return saved
  }
  return sessionStorage.getItem(TOKEN_KEY)
}
```

---

## 5단계 — 요청마다 토큰을 보여준다

`frontend/src/utils/token.js`

```js
export const authHeader = () => {
  const token = getToken()

  if (!token) {
    return {}   // 로그인 안 했으면 빈 객체
  }

  return { Authorization: `Bearer ${token}` }
}
```

API 함수가 이걸 헤더에 붙인다.

```js
export const getLikes = async () => {
  const response = await client.get('/api/likes', { headers: authHeader() })
  return response.data
}
```

실제로 나가는 요청:

```
GET /api/likes HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyODIi...
```

### 왜 매번 보내야 하나

서버가 **로그인 상태를 기억하지 않기 때문**이다. 세션 방식이라면 서버가 "이 세션
번호는 282번 회원" 이라는 표를 들고 있지만, JWT 방식은 그런 표가 없다. 그래서
요청마다 "나 이런 사람이야" 를 토큰으로 매번 증명한다. 이걸 **stateless** 라고 한다.

> 이 프로젝트는 axios 인터셉터를 쓰지 않는다. `api/*.js` 의 각 함수가
> `headers: authHeader()` 를 직접 붙인다. 한 군데라도 빠뜨리면 그 요청만 비회원으로
> 나가므로, 새 API 함수를 만들 때 챙겨야 한다.

### 로그인 안 해도 되는 API 는 어떻게 되나

`authHeader()` 가 빈 객체 `{}` 를 돌려주므로 헤더가 안 붙는다. 서버는 "비회원이
보냈구나" 로 보고 정상 처리한다. 추천 · 목록 조회가 그렇다.

---

## 6단계 — 서버가 토큰을 푼다

### 컨트롤러가 헤더를 받는다

```java
@GetMapping
public LikeListVO getList(
        @RequestHeader(value = "Authorization", required = false) String token) {
    UserVO loginUser = authService.getLoginUser(token);
    return likeService.getList(loginUser);
}
```

### 확인 함수가 셋이다

`backend/.../auth/service/AuthServiceImpl.java`

| 함수 | 토큰이 없거나 틀리면 | 쓰는 곳 |
|---|---|---|
| `getLoginUser(token)` | **401 에러** | 로그인 필수 (글쓰기 · 찜 · 내 정보) |
| `findLoginUser(token)` | **null** (비회원 취급) | 로그인 선택 (추천 · 상세 보기) |
| `getAdminUser(token)` | 관리자가 아니면 **403** | 공지 작성 · 수정 · 삭제 |

### 401 과 403 은 다르다

둘 다 "거절" 이지만 **이유가 반대**다.

| | 뜻 | 언제 |
|---|---|---|
| **401** Unauthorized | **네가 누군지 모르겠다** | 토큰이 없다 · 서명이 틀렸다 · 만료됐다 · 탈퇴한 회원이다 |
| **403** Forbidden | **누군지는 알겠는데 권한이 없다** | 로그인은 했지만 관리자가 아니다 |

```
만료된 토큰으로 찜 목록 조회     → 401   (다시 로그인하면 된다)
일반 회원이 공지 작성            → 403   (다시 로그인해도 소용없다)
```

**401 이 오면 "다시 로그인하세요", 403 이 오면 "권한이 없습니다"** 가 맞는 안내다.
반대로 쓰면 사용자가 로그인을 되풀이하면서 왜 안 되는지 모르게 된다.

`getAdminUser` 가 `getLoginUser` 를 먼저 부르는 이유도 이것이다. 로그인부터 확인해야
401 과 403 을 구분해서 던질 수 있다.

```java
public UserVO getAdminUser(String token) {
    UserVO user = getLoginUser(token);      // 먼저 401 판단
    if (!user.isAdmin()) {
        throw ApiException.forbidden("관리자만 할 수 있는 작업입니다.");  // 그 다음 403
    }
    return user;
}
```

```java
public UserVO getLoginUser(String token) {
    if (token == null || token.isBlank()) {
        throw ApiException.unauthorized("인증이 필요합니다.");
    }

    // 1. 토큰에서 회원 번호 꺼내기 ("Bearer " 뒤쪽이 진짜 토큰)
    Long userId = jwtUtil.getUserId(token.replace("Bearer ", "").trim());
    if (userId == null) {
        throw ApiException.unauthorized("토큰이 유효하지 않거나 만료되었습니다.");
    }

    // 2. 회원 번호로 회원 찾기
    UserVO user = userDAO.findById(userId);
    if (user == null || !"Y".equals(user.getUseYn())) {
        // 토큰을 받은 뒤에 탈퇴한 경우
        throw ApiException.unauthorized("존재하지 않는 사용자입니다.");
    }
    ...
}
```

```java
public UserVO findLoginUser(String token) {
    if (token == null || token.isBlank()) {
        return null;
    }
    try {
        return getLoginUser(token);
    } catch (ApiException e) {
        return null; // 토큰이 잘못돼도 로그인 안 한 사람으로 본다
    }
}
```

### 실제 검증은 여기서

`backend/.../util/JwtUtil.java`

```java
public Long getUserId(String token) {
    try {
        Claims claims = Jwts.parser()
                .verifyWith(getKey())      // ① 서명이 맞나
                .build()
                .parseSignedClaims(token)  // ② 만료되지 않았나
                .getPayload();

        return Long.valueOf(claims.getSubject());
    } catch (JwtException | IllegalArgumentException e) {
        return null;                       // 둘 중 하나라도 틀리면 null
    }
}
```

- `verifyWith(getKey())` — 받은 토큰의 ①②로 서명을 다시 계산해 ③과 비교한다
- `parseSignedClaims` — `exp` 가 지났으면 `ExpiredJwtException` 을 던진다
- 둘 다 `JwtException` 의 자식이라 `catch` 하나로 받는다

### 토큰을 믿고 끝내지 않는 이유

서명이 맞아도 **DB 를 한 번 더 본다.** 토큰은 한번 만들면 30분간 유효한데, 그
사이에 회원이 탈퇴할 수 있기 때문이다. `use_yn = 'N'` 이면 막는다.

이 때문에 "stateless 라면서 DB 를 왜 보냐" 는 말이 나올 수 있는데, 정확히는
**인증은 stateless, 인가는 DB 확인** 이다. 실무에서도 보통 이렇게 한다.

---

## 7단계 — 30분이 지나면

토큰이 만료되면 모든 API 가 401 을 돌려준다. 프론트는 앱이 켜질 때 한 번 확인한다.

`frontend/src/App.jsx`

```jsx
useEffect(() => {
  if (user === null) {
    return // 로그인 안 했으면 확인할 게 없다
  }

  const checkLogin = async () => {
    try {
      const me = await getMe()          // GET /api/auth/me
      handleUserChange(me)
    } catch (err) {
      // 401 이면 토큰이 만료된 것이다 → 로그아웃 상태로
      if (err.response && err.response.status === 401) {
        handleLogout()
      }
      // 서버가 꺼져 있는 경우는 저장해둔 정보를 그대로 쓴다
    }
  }
  checkLogin()
}, [])
```

`GET /api/auth/me` 는 "이 토큰이 아직 살아 있나" 를 묻는 용도로도 쓰인다.

**주의: 이 확인은 앱이 켜질 때 한 번뿐이다.** 화면을 켜둔 채로 30분이 지나면,
다음 API 호출에서 401 이 나고 화면에 에러 문구가 뜬다. 자동으로 로그인 화면으로
보내지는 않는다. (실무에서는 axios 인터셉터로 모든 401 을 가로채 로그아웃시킨다.)

---

## 부록 A. 로그아웃은 무슨 일을 하나

`frontend/src/App.jsx`

```jsx
const handleLogout = () => {
  removeToken()
  removeUser()
  setUser(null)
}
```

**서버에 아무 말도 안 한다.** 브라우저에 저장된 글자를 지울 뿐이다.

서버는 발급한 토큰 목록을 들고 있지 않으므로 "이 토큰 무효로 해줘" 라고 할
상대가 없다. 그래서 누군가 로그아웃 전에 토큰 글자를 복사해뒀다면 **남은 시간
동안은 그대로 쓸 수 있다.** JWT 의 가장 유명한 약점이다.

실무에서는 이렇게 보완한다.

| 방법 | 내용 |
|---|---|
| 짧은 만료 + refresh token | access 는 5~15분, 갱신용 refresh 를 따로 둔다 |
| 블랙리스트 | 로그아웃한 토큰을 Redis 에 넣고 매번 확인 (stateless 를 일부 포기) |
| 토큰 버전 | `USERS` 에 `token_version` 칸을 두고 토큰에도 넣어 비교 |

이 프로젝트는 만료를 30분으로 짧게 두는 것으로 갈음한다.

---

## 부록 B. 왜 스프링 시큐리티를 안 쓰나

`build.gradle` 에 들어 있는 것은 암호화 라이브러리 하나뿐이다.

```gradle
// 비밀번호 암호화(BCrypt)만 쓴다. 로그인 확인은 JWT 로 직접 한다.
implementation 'org.springframework.security:spring-security-crypto'
```

`spring-boot-starter-security` 가 **아니다.** 그래서 `SecurityFilterChain` ·
`@PreAuthorize` · `UserDetailsService` 같은 것이 하나도 없다.

### 장점

- 흐름이 전부 눈에 보인다. 컨트롤러 → 서비스 → DB 를 코드로 따라갈 수 있다
- 배울 개념이 적다 (필터 체인 · 인증 매니저 · 프로바이더를 몰라도 된다)

### 단점 — 이것 하나가 핵심이다

| | 기본값 |
|---|---|
| 스프링 시큐리티 | **전부 막아놓고** 열 곳을 지정 |
| 이 프로젝트 | **전부 열어놓고** 막을 곳에 코드를 씀 |

새 컨트롤러를 만들면서 `authService.getLoginUser(token)` 을 **깜빡하면 그 API 는
인증 없이 뚫린다.** 에러도 안 나고 테스트도 통과한다.

> 2026-10-05 기준으로 엔드포인트 54개를 전부 확인했고 빠뜨린 곳은 없었다.
> 새 엔드포인트를 만들 때마다 이걸 챙겨야 한다.

### 면접에서 물으면

> "스프링 시큐리티는 안 쓰고 JWT 로 직접 구현했습니다. 필터 체인 대신
> 컨트롤러에서 `Authorization` 헤더를 받아 `AuthService` 가 확인합니다.
> 엔드포인트마다 확인 코드를 써야 해서 빠뜨릴 위험이 있는 게 단점이고,
> 실무 규모에서는 시큐리티 필터로 JWT 를 처리하는 쪽이 안전합니다."

약점을 같이 말하는 것이 가장 좋은 답이다.

---

## 부록 C. 자주 나오는 질문

**Q. CSRF 공격은 안 막아도 되나?**

괜찮다. CSRF 는 브라우저가 **쿠키를 자동으로 붙여 보내는** 성질을 노린 공격이다.
이 프로젝트는 토큰을 쿠키가 아니라 `Authorization` 헤더에 **코드로 직접** 넣는다.
다른 사이트에서 몰래 요청을 보내도 헤더가 안 붙으므로 공격이 성립하지 않는다.

**Q. localStorage 에 토큰을 두는 게 위험하지 않나?**

XSS(악성 스크립트 삽입)가 뚫리면 `localStorage` 를 읽어갈 수 있다. 이것 때문에
"HttpOnly 쿠키에 두라" 는 의견이 많다. 다만 쿠키로 옮기면 CSRF 를 다시 막아야
한다. 어느 쪽도 공짜가 아니고, XSS 를 막는 것이 먼저다. React 는 기본적으로
문자열을 이스케이프하므로 `dangerouslySetInnerHTML` 만 안 쓰면 큰 위험은 없다.

**Q. 토큰이 길던데 매 요청마다 보내면 느리지 않나?**

이 프로젝트 토큰은 200자 안쪽이다. 요청 하나에 0.2KB 정도라 체감되지 않는다.
토큰에 권한 목록 같은 걸 잔뜩 넣으면 그때부터 문제가 된다.

**Q. 왜 `sub` 에 회원 번호만 넣고 닉네임은 안 넣나?**

넣어도 되지만 **바뀌는 값은 안 넣는 쪽이 낫다.** 닉네임을 토큰에 넣으면, 닉네임을
바꿔도 토큰 안의 값은 30분간 옛것으로 남는다. 변하지 않는 식별자(회원 번호)만
넣고 나머지는 DB 에서 읽는 것이 안전하다.

**Q. 서버를 재시작하면 로그인이 풀리나?**

안 풀린다. 서버가 토큰을 저장하지 않기 때문이다. 비밀키만 그대로면 전에 발급한
토큰도 계속 유효하다. **반대로 비밀키를 바꾸면 모든 사람이 즉시 로그아웃된다.**

---

## 부록 D. 직접 확인해보기

브라우저 F12 콘솔에서.

```js
// 1. 저장된 토큰 보기
localStorage.getItem('meokji.accessToken')

// 2. 내용 풀어보기 (암호화가 아니라 그냥 Base64 라는 증거)
const t = localStorage.getItem('meokji.accessToken')
JSON.parse(atob(t.split('.')[1]))
// → { sub: "282", iat: 1761234567, exp: 1761236367 }

// 3. 만료까지 남은 시간(초)
const p = JSON.parse(atob(t.split('.')[1]))
p.exp - Math.floor(Date.now() / 1000)

// 4. 토큰 한 글자를 고쳐서 보내보기 → 401 이 나야 정상
localStorage.setItem('meokji.accessToken', t.slice(0, -1) + 'X')
// 새로고침하면 로그아웃된다. 되돌리려면 다시 로그인.
```

서버 쪽은 `application.yml` 의 값을 바꿔보면 된다.

```yaml
app:
  jwt:
    expire-minutes: ${JWT_EXPIRE_MINUTES:30}   # 1 로 줄이면 1분 뒤 만료된다
```

---

## 한 줄 정리

> JWT 는 **"서버가 발급했고 내용이 고쳐지지 않았다" 를 서명으로 증명하는 글자**다.
> 내용을 감추지는 않는다. 서버는 그 글자를 저장하지 않고, 받을 때마다 서명과
> 만료만 확인한다. 그래서 가볍고 확장하기 좋은 대신, **한번 나간 토큰을 중간에
> 취소할 수 없다.**
