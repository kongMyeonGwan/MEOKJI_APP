# pages — 화면

주소 하나에 파일 하나다. **폴더 하나가 담당자 한 사람의 자리**가 되게 나눠뒀다.
내 폴더 안에서만 고치면 남의 작업과 부딪히지 않는다.

어느 주소에 어느 화면이 붙는지는 `src/App.jsx` 가 정한다.

```
pages/
├── home/         대시보드 (첫 화면)
├── auth/         로그인 · 회원가입 · 비밀번호 재설정
├── recommend/    추천 · 검색 · 예산 · 레시피 상세
├── game/         메뉴 정하기 게임
├── community/    게시판 · 공지 · 인기 레시피
├── mypage/       내 정보 · 기록 · 찜 · 설정
└── NotFoundPage.jsx    없는 주소. 어느 역할에도 속하지 않아 여기 둔다
```

---

## 폴더별로 무엇이 들어 있나

### `home/` — 대시보드

| 파일 | 주소 | 하는 일 |
|---|---|---|
| `HomePage.jsx` | `/` | 카드를 세로로 쌓아 보여준다 |

**다른 폴더의 컴포넌트를 많이 불러 쓴다.** 날씨 추천 · 시세 추천 · 미니게임 ·
예산 선택이 전부 여기 한 화면에 모인다. 카드 자체는
`components/recommendation/` 에 있으니, 카드 안을 고칠 일이면 그쪽을 본다.

### `auth/` — 로그인 · 가입

| 파일 | 주소 | 하는 일 |
|---|---|---|
| `LoginPage.jsx` | `/login` | 로그인 |
| `RegisterPage.jsx` | `/register` | 회원가입 |
| `PasswordResetPage.jsx` | `/password-reset` | 인증번호 3단계 (발송 → 확인 → 교체) |

로그인 상태는 화면이 직접 들고 있지 않다. `contexts/AuthContext` 가 갖고 있고
토큰은 `utils/token` 이 다룬다.

### `recommend/` — 추천 · 레시피

| 파일 | 주소 | 하는 일 |
|---|---|---|
| `RecommendPage.jsx` | `/recommend` | 조건으로 메뉴 고르기 |
| `BudgetPage.jsx` | `/budget` | 예산부터 정하고 고르기 |
| `SearchPage.jsx` | `/search` | 문장으로 찾기 |
| `RecipePage.jsx` | `/recipe/:foodName` | 레시피 + 재료 최저/최고가 |

`RecipePage` 가 가장 크다. 가격표는 `components/price/` 에 따로 있다.

### `game/` — 메뉴 정하기

| 파일 | 주소 | 하는 일 |
|---|---|---|
| `GamePage.jsx` | `/game` | 게임 고르는 화면 |
| `WorldCupPage.jsx` | `/game/worldcup` | 음식 월드컵 |
| `BalancePage.jsx` | `/game/balance` | 밸런스 게임 |
| `RoulettePage.jsx` | `/roulette` | 룰렛 |

게임에 나오는 후보와 질문은 코드에 박혀 있지 않다. 서버가 주는
`GET /api/catalog` 를 `contexts/CatalogContext` 가 받아둔다.

### `community/` — 게시판 · 공지

| 파일 | 주소 | 하는 일 |
|---|---|---|
| `CommunityPage.jsx` | `/community` | 글 목록 · 공지 |
| `PostDetailPage.jsx` | `/community/:postId` | 글 + 댓글 + 좋아요 |
| `PostWritePage.jsx` | `/community/write` | 글쓰기 (로그인 필요) |
| `PopularPage.jsx` | `/popular` | 인기 레시피 |

> **아직 서버가 없다.** 게시판과 공지 API 는 501 이고, 이 화면들은
> `src/data/sampleBoard.js` 의 가데이터로 그려진다. 서버가 완성되면
> `SAMPLE_*` 대신 `api/posts.js` · `api/notices.js` 를 부르게 바꾸고
> `sampleBoard.js` 를 지우면 된다.
>
> 서버 쪽이 어떻게 동작해야 하는지는 `docs/BOARD_SPEC.md` 에 있다.

### `mypage/` — 내 정보

| 파일 | 주소 | 하는 일 |
|---|---|---|
| `MyPage.jsx` | `/mypage` | 내 정보 · 못 먹는 재료 · 알림 · 탈퇴 |
| `HistoryPage.jsx` | `/history` | 식사 기록 |
| `AiHistoryPage.jsx` | `/ai-history` | 추천받은 기록 (브라우저에만 저장) |
| `LikedPage.jsx` | `/liked` | 찜한 레시피 |
| `SettingsPage.jsx` | `/settings` | 테마 · 알림 |

`SettingsPage` 만 **로그인 없이** 쓸 수 있다. 브라우저에 저장되는 값이라서다.
나머지는 `ProtectedRoute` 안에 있다.

---

## 고칠 때 알아둘 것

### 상대경로가 한 겹 더 깊다

화면이 폴더 안으로 들어가서 `../` 가 아니라 `../../` 다.

```jsx
import Loading from '../../components/common/Loading'
import { useAuth } from '../../contexts/AuthContext'
import '../../styles/pages/MyPage.css'
```

### 스타일은 아직 한 곳에 모여 있다

`styles/pages/*.css` 는 나누지 않았다. 파일 이름이 화면 이름과 같으니
`MyPage.jsx` 는 `styles/pages/MyPage.css` 를 본다.

여러 목록 화면이 `ListPage.css` 를 같이 쓴다(11개). **고칠 때 다른 화면이
같이 바뀐다** — 한 화면만 바꾸려면 그 화면 전용 CSS 를 새로 만든다.

### 화면을 새로 만들면

1. 맡은 폴더에 `XxxPage.jsx` 를 만든다
2. `src/App.jsx` 에 import 와 `<Route>` 를 추가한다 — **두 곳 다 고쳐야 한다**
3. 로그인이 필요하면 `<ProtectedRoute>` 안에 넣는다
4. 사이드바·하단 탭에도 보이게 하려면 `components/common/` 의 메뉴를 고친다

### 화면끼리 직접 부르지 않는다

지금 `pages/` 안에서 서로를 import 하는 곳은 한 군데도 없다. 화면을 부르는 건
`App.jsx` 뿐이다. 이 규칙이 지켜져야 폴더를 옮겨도 남의 화면이 안 깨진다.

여러 화면이 같이 쓰는 조각은 `components/` 로 뺀다.
