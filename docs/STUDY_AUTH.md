# 인증(로그인 + JWT) 학습 가이드

> ⚠️ **이 문서는 옛 구조를 기준으로 쓴 학습 기록이다.** 아래에 나오는
> `security/JwtProvider` · `@LoginUser` · 인자 리졸버 · `config/AppProperties` ·
> `config/SecurityConfig` · 프론트의 `contexts/AuthContext.jsx` ·
> `ProtectedRoute.jsx` 는 **지금 코드에 없다.**
>
> 지금은 컨트롤러가 `@RequestHeader("Authorization")` 로 토큰을 받아
> `authService.getLoginUser(token)` / `findLoginUser` / `getAdminUser` 를 부르고,
> 자바 폴더는 기능별(`auth/controller` · `auth/service` · `auth/dao` · `auth/vo` ·
> `auth/dto`)로 나뉘어 있다. 흐름을 이해하는 용도로만 읽고, 파일 경로는
> `backend-spring/README.md` 를 본다.

이 프로젝트의 인증 코드를 **자기 것으로 만들기 위한** 문서다.
읽기만 해서는 남지 않는다. 아래 순서대로 직접 해봐야 한다.

```
1단계  흐름을 끝까지 따라가기      (30분)
2단계  일부러 깨뜨려보기            (1시간)   ← 가장 효과가 크다
3단계  질문에 말로 답해보기         (30분)
4단계  백지에서 다시 써보기         (2~3시간) ← 진짜 시험
```

---

## 1단계. 흐름을 끝까지 따라가기

### 로그인 버튼을 누르면 일어나는 일

번호 순서대로 파일을 열어 그 줄을 직접 눈으로 확인한다.

```
[프론트엔드]
1  pages/auth/LoginPage.jsx        onSubmit -> login(email, password)
2  contexts/AuthContext.jsx        login() -> loginApi() 호출
3  api/auth.js                     client.post('/api/auth/login', {...})
4  api/client.js                   요청 인터셉터 (아직 토큰 없음)
                                        |
                                     HTTP POST
                                        |
[백엔드 — backend-spring]
5  controller/AuthController       login()  - HTTP 껍데기
6  dto/auth/LoginRequest           @Valid 가 요청 본문을 검증
7  service/AuthService             login() - 이 사람이 맞는가 판정
8  config/SecurityConfig           PasswordEncoder(bcrypt) 로 대조
9  security/JwtProvider            createAccessToken() 토큰 생성
10 dto/auth/TokenResponse          응답 조립 (UserResponse.from)
                                        |
                                     200 OK
                                        |
[프론트엔드]
11 contexts/AuthContext.jsx        localStorage 저장 + setAuthToken() + setUser()
12 api/client.js                   이제부터 모든 요청에 Authorization 헤더가 붙는다
13 components/common/TopBar.jsx    isAuthenticated 가 true -> 닉네임 표시
```

### 보호된 API를 부르면 일어나는 일

```
1  api/client.js                     요청 인터셉터가 Authorization: Bearer xxx 를 붙임
                                          |
2  security/LoginUserArgumentResolver  헤더에서 "Bearer " 를 떼고 토큰만 뽑음
3  security/JwtProvider               readUserId() - 서명·만료 검증
4  mapper/UserMapper                  findById() 로 회원을 읽음
5  LoginUserArgumentResolver          use_yn · 잠금 · 관리자 여부 확인
6  controller/...                     @LoginUser User me 로 받아서 처리
```

**필터가 아니라 인자 리졸버다.** `SecurityConfig` 에는 주소 목록이 없다.
"이 엔드포인트에 로그인이 필요한가" 를 컨트롤러 선언부에서 읽을 수 있게 한 것이다.

```java
@LoginUser User me                      // 없으면 401
@LoginUser(required = false) User me    // 게스트도 가능. 없으면 null
@LoginUser(admin = true) User me        // user_level=1 아니면 403
```

### 새로고침했는데 로그인이 유지되는 이유

```
1  contexts/AuthContext.jsx     useEffect -> localStorage 에서 토큰 꺼냄
2                               setAuthToken(saved)
3  api/auth.js                  getMe() 로 서버에 "이 토큰 아직 살아있나?" 확인
4                               200 이면 user 복원 / 401 이면 토큰 버림
5                               그동안 restoring = true (아직 모름 상태)
```

**여기서 `restoring` 이 왜 필요한지 반드시 이해하고 넘어갈 것.** 2단계에서 깨뜨려본다.

---

## 2단계. 일부러 깨뜨려보기 (가장 중요)

각 실험마다 **먼저 결과를 예측해서 적고**, 그다음 실행한다.
예측이 틀렸다면 그 부분을 모르고 있었던 것이다.

준비:

```
터미널 1:  cd backend-spring  →  gradlew bootRun          (8080)
터미널 2:  cd frontend        →  npm run dev              (5173)
```

> 고친 뒤에는 **서버를 다시 띄워야** 한다. 자바는 파이썬의 `--reload` 같은 게
> 기본으로 없다. VSCode 에서 F5 로 띄웠다면 재시작 버튼(`Ctrl+Shift+F5`)이 빠르다.

### 실험 1 — JWT 비밀키를 바꾼다

`backend-spring/src/main/resources/application-local.yml` 의
`app.jwt.secret-key` 뒤에 아무 글자나 붙이고 **서버 재시작**.

- 예상: 이미 로그인된 상태에서 새로고침하면 ?
- 확인할 것: 왜 그렇게 되나? 서버는 토큰을 어디에 저장해두고 비교하는가?
- **핵심**: JWT는 서버가 저장하지 않는다. **서명을 다시 계산해서 맞는지 볼 뿐이다.**

> 키를 32자보다 짧게 만들면 아예 뜨지 않는다. `JwtProvider.init()` 이
> `@PostConstruct` 에서 `WeakKeyException` 을 낸다. 토큰을 만들 때마다 실패하는
> 것보다 시작할 때 멈추는 편이 낫다는 판단이다.

### 실험 2 — 토큰 내용을 직접 읽어본다

브라우저 F12 → Application → Local Storage → `meokji.accessToken` 값 복사 →
<https://jwt.io> 에 붙여넣기.

- 예상: 비밀키 없이 내용이 보일까?
- **핵심**: JWT는 **암호화가 아니라 서명**이다. 누구나 읽을 수 있고, 고칠 수 없을 뿐이다.
  그래서 `createAccessToken()` 에 비밀번호나 개인정보를 넣으면 안 된다.
  이 프로젝트가 payload 에 `sub`(id)만 넣은 이유다.

### 실험 3 — 토큰을 한 글자 고친다

Local Storage 값의 **마지막 글자 하나만** 바꾸고 새로고침.

- 예상: 어떤 화면이 보일까? 서버 로그에는 뭐가 찍힐까?
- 확인: `JwtProvider.readUserId()` 가 무엇을 돌려주고,
  `LoginUserArgumentResolver.findByToken()` 의 어느 줄에서 401 이 나는가?

### 실험 4 — 만료 시간을 1분으로 줄인다

`application-local.yml` 에 `app.jwt.expire-minutes: 1` 을 넣고 재시작 →
로그인 → 1분 기다림 → `/mypage` 이동.

- 예상: 무슨 일이 벌어지나?
- 확인: `api/client.js` 의 `onUnauthorized` 가 언제 불리는가? 자동 로그아웃되는가?

### 실험 5 — `restoring` 을 없애본다

`contexts/AuthContext.jsx` 에서 `restoring` 의 초깃값을 `false` 로 바꾼다.

```js
const [restoring, setRestoring] = useState(false)   // 원래는 true
```

로그인한 상태로 `/history` 에서 **새로고침**.

- 예상: 무엇이 달라지나?
- **핵심**: "로그인 안 함" 과 "아직 모름" 은 다른 상태다.
  둘을 구분하지 않으면 로그인했는데도 `/login` 으로 튕긴다.
- 확인 후 반드시 되돌릴 것.

### 실험 6 — 토큰 주입을 빼먹는다

`AuthContext.jsx` 의 `login()` 안에서 `setAuthToken(accessToken)` 줄을 주석 처리.

- 예상: 로그인은 될까? 그다음 `/mypage` 는?
- **핵심**: 토큰을 받는 것과 **다음 요청에 실어 보내는 것**은 별개의 일이다.

### 실험 7 — 잠금 검사를 지운다

`security/LoginUserArgumentResolver` 의 `findByToken()` 에서
`if (user.isLockedAccount())` 블록을 주석 처리.
계정을 20번 실패로 잠근 뒤, **잠기기 전에 받아둔 토큰**으로 API를 호출.

- 예상: 통과할까?
- **핵심**: JWT는 발급 후 서버가 취소할 수 없다. 그래서 **요청마다 DB를 확인**한다.
  이게 JWT의 가장 큰 약점이다. 바로 위의 `use_yn` 검사(탈퇴)도 같은 이유다.

### 실험 8 — 리졸버 등록을 빼본다 (스프링만의 함정)

`config/WebConfig` 에서 `addArgumentResolvers(...)` 안의 등록 한 줄을 주석 처리하고
재시작 → 로그인 없이 `GET /api/users/me` 를 불러본다.

- 예상: 401 이 날까, 아니면 다른 일이 벌어질까?
- **핵심**: 등록을 빼면 `@LoginUser User me` 에 스프링이 **조용히 null 을 넣는다.**
  에러가 안 난다. 로그인 검사가 통째로 사라진 채로 정상 동작하는 것처럼 보인다.
  `AuthController.me()` 에 `if (me == null)` 검사가 한 번 더 있는 이유가 이것이다.
- 확인 후 반드시 되돌릴 것.

### 실험 9 — 비밀번호 해시를 눈으로 본다

DBeaver 에서:

```sql
SELECT email, password_hash FROM users;
```

같은 비밀번호로 계정을 두 개 만들어 해시를 비교해본다.

- 예상: 두 해시가 같을까 다를까?
- **핵심**: bcrypt 는 매번 다른 salt 를 섞는다. 그래서 해시가 달라도 검증은 된다.
  `PasswordEncoder.matches()` 가 "복호화" 하지 않고 "대조" 하는 이유.

> 해시가 `$2b$...` 로 시작하는 것을 확인해두면 좋다. 파이썬(passlib)이 만든 것과
> 같은 형식이라, 스프링으로 옮긴 뒤에도 **기존 회원이 그대로 로그인된다.**

---

## 3단계. 말로 답해보기

**보지 않고** 답할 수 있어야 한다. 막히면 그 부분을 다시 읽는다.

### JWT 자체

1. 세션 방식과 JWT 방식의 가장 큰 차이는?
2. 서버는 발급한 토큰을 어디에 저장하는가?
3. 로그아웃을 서버 API 없이 프론트에서만 처리해도 되는 이유는?
4. 그 방식의 단점은? (힌트: 실험 7)
5. `sub` / `iat` / `exp` 는 각각 무엇인가?
6. `sub` 에 id 를 **문자열로** 넣는 이유는? (힌트: RFC 7519)

### 이 프로젝트의 선택

7. 왜 토큰에 닉네임을 안 넣었나?
8. 왜 Refresh Token 을 안 쓰나? 대신 무엇으로 위험을 줄였나?
9. 로그인 실패 시 "없는 이메일" 과 "틀린 비밀번호" 를 왜 구분하지 않나?
10. 그런데 왜 "남은 시도 N회" 는 알려주기로 했나? 무엇을 포기한 것인가?
11. `@LoginUser` 와 `@LoginUser(required = false)` 는 언제 각각 쓰나?
12. 권한 검사를 `SecurityConfig` 에 URL 목록으로 적지 않은 이유는?
13. 왜 필터가 아니라 `HandlerMethodArgumentResolver` 인가?

### 스프링

14. `PasswordEncoder` 를 `SecurityConfig` 의 `@Bean` 으로 둔 이유는?
15. `JwtProvider` 가 생성자가 아니라 `@PostConstruct` 에서 키를 만드는 이유는?
16. `@Transactional` 을 컨트롤러가 아니라 서비스에 붙이는 이유는?
17. 오류를 `ResponseEntity` 로 직접 만들지 않고 `ApiException` 으로 던지는 이유는?

### 프론트엔드

18. `setAuthToken()` 이 axios 인스턴스 밖의 모듈 변수인 이유는?
19. `client.js` 가 `AuthContext` 를 직접 import 하지 않고 콜백을 받는 이유는?
20. `ProtectedRoute` 는 왜 컴포넌트가 아니라 라우트를 감싸는 형태인가?
21. localStorage 대신 HttpOnly 쿠키를 쓰면 뭐가 좋아지고 뭐가 번거로워지나?

### 비밀번호

22. bcrypt 해시를 "복호화" 할 수 있나?
23. salt 는 왜 필요한가?
24. 왜 비밀번호 길이를 문자 수가 아니라 **바이트**로 검사하나? (`util/Passwords`)
25. 비밀번호 재설정에서 `used_at` 컬럼이 없으면 무슨 문제가 생기나?

---

## 4단계. 백지에서 다시 써보기

진짜 시험이다. **이 프로젝트를 보지 않고** 새 폴더에 최소 로그인을 만든다.

### 규칙

- 이 저장소를 열어보지 않는다 (막히면 30분은 스스로 해결해본다)
- DB 는 H2 로 해도 된다. 목적은 인증 흐름이지 Oracle 이 아니다
- 화면은 못생겨도 된다

### 순서

```
1  start.spring.io 로 프로젝트 만들기 (Web, Security, MyBatis 또는 JPA)
2  GET /api/health 띄우기
3  User 테이블 (email, password_hash) + 회원가입
4  BCryptPasswordEncoder 로 해싱
5  POST /api/auth/login  -> 토큰 문자열 반환 (jjwt)
6  GET /api/auth/me      -> 토큰 검증해서 회원 돌려주기
7  React: 로그인 폼 -> 토큰 localStorage 저장
8  axios 인터셉터로 Authorization 자동 첨부
9  새로고침해도 로그인 유지
10 ProtectedRoute
```

6번을 **먼저 필터로** 만들어보고, 그다음 인자 리졸버로 바꿔보면 좋다.
두 방식의 차이가 몸으로 남는다.

### 다 만든 뒤

이 프로젝트의 같은 파일을 열어 **내 코드와 비교**한다.
다른 부분마다 "왜 다르게 했을까" 를 답해본다.

- 내가 놓친 것인가?
- 내 방식이 더 단순한가?
- 상황이 달라서인가?

---

## 참고: 각 파일의 역할 한 줄 요약

| 파일 | 한 줄 |
|---|---|
| `config/AppProperties` | `application.yml` 의 `app.*` 를 담는 객체. 설정의 단일 진입점 |
| `config/SecurityConfig` | 세션·폼로그인을 끄고 `PasswordEncoder` 와 CORS 만 둔다 |
| `config/WebConfig` | 인자 리졸버를 등록한다. **빼먹으면 조용히 열린다** (실험 8) |
| `security/JwtProvider` | 토큰 발급/해석. **DB도 HTTP도 모른다** |
| `security/@LoginUser` | "이 인자에 로그인한 회원을 넣어달라" 는 표시 |
| `security/LoginUserArgumentResolver` | 요청 헤더 -> User 객체. 컨트롤러가 토큰을 직접 다루지 않게 함 |
| `dto/auth/` | 요청/응답의 모양과 검증 규칙 |
| `service/AuthService` | "이 사람이 맞는가" 판정 + 실패 횟수·잠금 |
| `controller/AuthController` | HTTP 껍데기. 서비스 호출 + 응답 변환뿐 |
| `common/ApiException` | 화면에 그대로 보여줄 오류. `{"detail": ...}` 로 나간다 |
| `contexts/AuthContext.jsx` | 로그인 상태를 앱 전체에 공유 |
| `api/client.js` | axios 인스턴스 + 토큰 자동 첨부 + 에러 정규화 |
| `components/common/ProtectedRoute.jsx` | 로그인 안 했으면 `/login` 으로 |

### 계층을 나눈 이유

```
controller  →  service  →  mapper  →  DB
  (HTTP)      (업무 규칙)    (SQL)
```

`AuthService.login()` 은 **HTTP 를 모른다.** `HttpServletRequest` 도 상태 코드도
다루지 않는다. 그래서 나중에 다른 입구(배치, 소켓)에서도 그대로 부를 수 있다.

컨트롤러는 반대로 **업무 규칙을 모른다.** 무엇이 로그인 실패인지, 몇 번 틀리면
잠기는지는 서비스가 정한다.

> 이 규칙에 예외를 두지 않았다. 게임 기록처럼 한 줄 넣는 일도 `GameService` 를
> 거친다 — 어떤 컨트롤러는 거치고 어떤 것은 매퍼를 직접 부르면, 새로 들어온
> 사람이 규칙을 코드마다 다르게 배운다.

---

## 파이썬 시절과 달라진 곳

이 프로젝트는 원래 FastAPI 였고 2026-09 에 스프링부트로 옮겼다. 인증의 **판단은
그대로**고 그것을 놓는 자리만 바뀌었다.

| 하는 일 | 파이썬 | 스프링 |
|---|---|---|
| 설정 읽기 | `core/config.py` | `config/AppProperties` |
| 비밀번호 해싱 | `passlib` | `BCryptPasswordEncoder` |
| 토큰 발급·해석 | `core/security.py` | `security/JwtProvider` |
| 헤더 -> 회원 | `Depends(get_current_user)` | `@LoginUser` + 인자 리졸버 |
| 게스트 허용 | `Depends(get_optional_user)` | `@LoginUser(required = false)` |
| 관리자만 | `Depends(get_admin_user)` | `@LoginUser(admin = true)` |

**토큰은 서로 통한다.** 같은 HS256, 같은 비밀키, `sub` 에 id 를 문자열로 담는
것까지 같게 맞췄다. 비밀번호 해시도 그대로 쓴다.

옮기던 시절의 판단과 걸렸던 것들은 `docs/PROGRESS.md` 의 「서버를 스프링부트로
옮기는 중」 절에 있다.
