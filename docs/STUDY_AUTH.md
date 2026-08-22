# 인증(로그인 + JWT) 학습 가이드

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
1  pages/LoginPage.jsx          onSubmit -> login(email, password)
2  contexts/AuthContext.jsx     login() -> loginApi() 호출
3  api/auth.js                  client.post('/api/auth/login', {...})
4  api/client.js                요청 인터셉터 (아직 토큰 없음)
                                     |
                                  HTTP POST
                                     |
[백엔드]
5  routers/auth.py              login()  - HTTP 껍데기
6  schemas/auth.py              LoginRequest 로 요청 본문 검증
7  services/auth_service.py     login_user() - 이 사람이 맞는가 판정
8  core/security.py             verify_password() bcrypt 대조
9  core/security.py             create_access_token() 토큰 생성
10 schemas/auth.py              TokenResponse 로 응답 조립
                                     |
                                  200 OK
                                     |
[프론트엔드]
11 contexts/AuthContext.jsx     localStorage 저장 + setAuthToken() + setUser()
12 api/client.js                이제부터 모든 요청에 Authorization 헤더가 붙는다
13 components/common/TopBar.jsx isAuthenticated 가 true -> 닉네임 표시
```

### 보호된 API를 부르면 일어나는 일

```
1  api/client.js                요청 인터셉터가 Authorization: Bearer xxx 를 붙임
                                     |
2  core/deps.py                  HTTPBearer 가 헤더에서 토큰만 뽑아냄
3  core/deps.py                  _resolve_user() -> decode_token()
4  core/security.py              jwt.decode() 서명·만료 검증
5  core/deps.py                  payload['sub'] -> db.get(User, id)
6  routers/auth.py               current_user 를 받아서 처리
```

### 새로고침했는데 로그인이 유지되는 이유

```
1  contexts/AuthContext.jsx     useEffect -> localStorage 에서 토큰 꺼냄
2                               setAuthToken(saved)
3  api/auth.js                  getMe() 로 서버에 "이 토큰 아직 살아있나?" 확인
4                               200 이면 user 복원 / 401 이면 토큰 버림
5                               그동안 restoring = true (아직 모름 상태)
```

**여기서 `restoring`이 왜 필요한지 반드시 이해하고 넘어갈 것.** 2단계에서 깨뜨려본다.

---

## 2단계. 일부러 깨뜨려보기 (가장 중요)

각 실험마다 **먼저 결과를 예측해서 적고**, 그다음 실행한다.
예측이 틀렸다면 그 부분을 모르고 있었던 것이다.

준비:
```
터미널 1:  cd backend  → .venv\Scripts\activate → uvicorn app.main:app --reload --port 8000
터미널 2:  cd frontend → npm run dev
```

### 실험 1 — JWT 비밀키를 바꾼다

`backend/.env` 의 `JWT_SECRET_KEY` 뒤에 아무 글자나 붙이고 **서버 재시작**.
(`.env`는 시작할 때만 읽으므로 `--reload`로는 반영 안 된다)

- 예상: 이미 로그인된 상태에서 새로고침하면 ?
- 확인할 것: 왜 그렇게 되나? 서버는 토큰을 어디에 저장해두고 비교하는가?
- **핵심**: JWT는 서버가 저장하지 않는다. **서명을 다시 계산해서 맞는지 볼 뿐이다.**

### 실험 2 — 토큰 내용을 직접 읽어본다

브라우저 F12 → Application → Local Storage → `meokji.accessToken` 값 복사 →
https://jwt.io 에 붙여넣기.

- 예상: 비밀키 없이 내용이 보일까?
- **핵심**: JWT는 **암호화가 아니라 서명**이다. 누구나 읽을 수 있고, 고칠 수 없을 뿐이다.
  그래서 `create_access_token()`에 비밀번호나 개인정보를 넣으면 안 된다.
  이 프로젝트가 payload에 `sub`(id)만 넣은 이유다.

### 실험 3 — 토큰을 한 글자 고친다

Local Storage 값의 **마지막 글자 하나만** 바꾸고 새로고침.

- 예상: 어떤 화면이 보일까? 서버 로그에는 뭐가 찍힐까?
- 확인: `core/deps.py`의 어느 줄에서 걸리는가?

### 실험 4 — 만료 시간을 1분으로 줄인다

`.env`의 `JWT_EXPIRE_MINUTES=1` → 서버 재시작 → 로그인 → 1분 기다림 → `/mypage` 이동.

- 예상: 무슨 일이 벌어지나?
- 확인: `api/client.js`의 `onUnauthorized` 가 언제 불리는가? 자동 로그아웃되는가?

### 실험 5 — `restoring`을 없애본다

`contexts/AuthContext.jsx` 에서 `restoring`의 초깃값을 `false`로 바꾼다.

```js
const [restoring, setRestoring] = useState(false)   // 원래는 true
```

로그인한 상태로 `/history`에서 **새로고침**.

- 예상: 무엇이 달라지나?
- **핵심**: "로그인 안 함"과 "아직 모름"은 다른 상태다.
  둘을 구분하지 않으면 로그인했는데도 `/login`으로 튕긴다.
- 확인 후 반드시 되돌릴 것.

### 실험 6 — 토큰 주입을 빼먹는다

`AuthContext.jsx`의 `login()` 안에서 `setAuthToken(accessToken)` 줄을 주석 처리.

- 예상: 로그인은 될까? 그다음 `/mypage`는?
- **핵심**: 토큰을 받는 것과 **다음 요청에 실어 보내는 것**은 별개의 일이다.

### 실험 7 — 잠금 검사를 지운다

`core/deps.py`의 `if user.is_locked:` 블록을 주석 처리.
계정을 20번 실패로 잠근 뒤, 잠기기 전에 받아둔 토큰으로 API를 호출.

- 예상: 통과할까?
- **핵심**: JWT는 발급 후 서버가 취소할 수 없다. 그래서 **요청마다 DB를 확인**한다.
  이게 JWT의 가장 큰 약점이다.

### 실험 8 — 비밀번호 해시를 눈으로 본다

DBeaver에서:
```sql
SELECT email, password_hash FROM users;
```
같은 비밀번호로 계정을 두 개 만들어 해시를 비교해본다.

- 예상: 두 해시가 같을까 다를까?
- **핵심**: bcrypt는 매번 다른 salt를 섞는다. 그래서 해시가 달라도 검증은 된다.
  `verify_password()`가 "복호화"하지 않고 "대조"하는 이유.

---

## 3단계. 말로 답해보기

**보지 않고** 답할 수 있어야 한다. 막히면 그 부분을 다시 읽는다.

### JWT 자체

1. 세션 방식과 JWT 방식의 가장 큰 차이는?
2. 서버는 발급한 토큰을 어디에 저장하는가?
3. 로그아웃을 서버 API 없이 프론트에서만 처리해도 되는 이유는?
4. 그 방식의 단점은? (힌트: 실험 7)
5. `sub` / `iat` / `exp` 는 각각 무엇인가?
6. `jwt.decode()`에 `algorithms=[...]`를 반드시 넘겨야 하는 이유는?

### 이 프로젝트의 선택

7. 왜 토큰에 닉네임을 안 넣었나?
8. 왜 Refresh Token을 안 쓰나? 대신 무엇으로 위험을 줄였나?
9. 로그인 실패 시 "없는 이메일"과 "틀린 비밀번호"를 왜 구분하지 않나?
10. 그런데 왜 "남은 시도 N회"는 알려주기로 했나? 무엇을 포기한 것인가?
11. `get_current_user`와 `get_optional_user`는 언제 각각 쓰나?
12. `HTTPBearer(auto_error=False)`의 `auto_error`가 하는 일은?

### 프론트엔드

13. `setAuthToken()`이 axios 인스턴스 밖의 모듈 변수인 이유는?
14. `client.js`가 `AuthContext`를 직접 import하지 않고 콜백을 받는 이유는?
15. `ProtectedRoute`는 왜 컴포넌트가 아니라 라우트를 감싸는 형태인가?
16. localStorage 대신 HttpOnly 쿠키를 쓰면 뭐가 좋아지고 뭐가 번거로워지나?

### 비밀번호

17. bcrypt 해시를 "복호화"할 수 있나?
18. salt는 왜 필요한가?
19. 왜 비밀번호 길이를 문자 수가 아니라 **바이트**로 검사하나?
20. 비밀번호 재설정에서 `used_at` 컬럼이 없으면 무슨 문제가 생기나?

---

## 4단계. 백지에서 다시 써보기

진짜 시험이다. **이 프로젝트를 보지 않고** 새 폴더에 최소 로그인을 만든다.

### 규칙

- 이 저장소를 열어보지 않는다 (막히면 30분은 스스로 해결해본다)
- DB는 SQLite로 해도 된다. 목적은 인증 흐름이지 Oracle이 아니다
- 화면은 못생겨도 된다

### 순서

```
1  FastAPI 띄우기 + /api/health
2  User 모델 (email, password_hash) + 회원가입
3  bcrypt 해싱 (passlib 말고 bcrypt 직접)
4  POST /login  -> 토큰 문자열 반환
5  GET /me      -> Depends 로 토큰 검증
6  React: 로그인 폼 -> 토큰 localStorage 저장
7  axios 인터셉터로 Authorization 자동 첨부
8  새로고침해도 로그인 유지
9  ProtectedRoute
```

### 다 만든 뒤

이 프로젝트의 같은 파일을 열어 **내 코드와 비교**한다.
다른 부분마다 "왜 다르게 했을까"를 답해본다.
- 내가 놓친 것인가?
- 내 방식이 더 단순한가?
- 상황이 달라서인가?

---

## 참고: 각 파일의 역할 한 줄 요약

| 파일 | 한 줄 |
|---|---|
| `core/config.py` | `.env` 값을 타입 검증된 객체로. 설정의 단일 진입점 |
| `core/security.py` | 비밀번호 해싱 + 토큰 발급/해석. **DB도 HTTP도 모른다** |
| `core/deps.py` | 요청 헤더 -> User 객체. 라우터가 토큰을 직접 다루지 않게 함 |
| `schemas/auth.py` | 요청/응답의 모양과 검증 규칙 |
| `services/auth_service.py` | "이 사람이 맞는가" 판정. **HTTP를 모른다** (사실 HTTPException은 쓰지만) |
| `routers/auth.py` | HTTP 껍데기. 서비스 호출 + 응답 변환뿐 |
| `contexts/AuthContext.jsx` | 로그인 상태를 앱 전체에 공유 |
| `api/client.js` | axios 인스턴스 + 토큰 자동 첨부 + 에러 정규화 |
| `components/common/ProtectedRoute.jsx` | 로그인 안 했으면 `/login`으로 |

### 계층을 나눈 이유

```
router   →   service   →   model
 (HTTP)    (비즈니스 로직)   (DB)
```

`login_user()`는 HTTP를 몰라도 되게 짜여 있다. 그래서
- 테스트할 때 서버를 안 띄워도 된다
- 나중에 CLI나 배치에서도 같은 함수를 쓸 수 있다

`routers/auth.py`가 3~5줄밖에 안 되는 것이 이 구조가 지켜지고 있다는 신호다.
라우터가 길어지기 시작하면 로직이 새어 나온 것이다.
