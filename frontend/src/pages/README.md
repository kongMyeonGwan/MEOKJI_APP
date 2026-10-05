# pages — 화면

주소 하나에 파일 하나다. **폴더 하나가 담당자 한 사람의 자리**가 되게 나눠뒀다.
내 폴더 안에서만 고치면 남의 작업과 부딪히지 않는다.

어느 주소에 어느 화면이 붙는지는 `src/App.jsx` 가 정한다.

기능 폴더 안에는 화면(`XxxPage.jsx`)과 **그 기능에서만 쓰는 조각(`components/`)** 이
같이 들어 있다. 스프링에서 `auth/` 패키지 안에 controller · service · dao · vo 를
같이 두는 것과 같은 생각이다. 여러 기능이 같이 쓰는 조각만 `src/components/common/` 에 있다.

```
pages/
├── home/         대시보드 (첫 화면)
├── auth/         로그인 · 회원가입 · 비밀번호 재설정
├── recommend/    추천 · 검색 · 예산 · 레시피 상세
├── game/         메뉴 정하기 게임
├── community/    게시판 셋. 아래에 또 나눠져 있다
│   ├── recipeReview/   레시피리뷰 · 인기 레시피
│   ├── free/           자유게시판
│   └── notice/         공지사항
├── mypage/       내 정보 · 기록 · 찜 · 설정
└── NotFoundPage.jsx    없는 주소. 어느 역할에도 속하지 않아 여기 둔다
```

`community/` 만 한 겹 더 깊다. 게시판이 셋인데 화면 모양과 쓰는 가데이터가
비슷해서 한 자리에 모았고, 그 안에서 **담당자별로 다시 나눴다.** 세 폴더가
각각 화면(`XxxPage.jsx`)과 `components/` 를 따로 가진다.

```
community/recipeReview/
├── ReviewListPage.jsx      /community
├── ReviewDetailPage.jsx    /community/:reviewId
├── ReviewWritePage.jsx     /community/write
├── PopularPage.jsx         /popular
└── components/             ReviewCard · ReviewAttachment · CommentItem
```

게시판 화면 이름은 **역할을 뒤에 붙인다.** 셋이 한 벌이라 파일 이름만 보고
어느 화면인지 알 수 있어야 한다.

| 역할 | 이름 | 예 |
|---|---|---|
| 목록 | `XxxListPage` | `FreeListPage` · `NoticeListPage` · `ReviewListPage` |
| 상세 | `XxxDetailPage` | `FreeDetailPage` · `NoticeDetailPage` · `ReviewDetailPage` |
| 작성 | `XxxWritePage` | `FreeWritePage` · `NoticeWritePage` · `ReviewWritePage` |

`PopularPage` 는 이 셋에 안 들어간다. 게시판의 목록이 아니라 좋아요 순으로
따로 뽑아 보여주는 화면이라 그대로 둔다.

주소는 폴더 이름을 따라가지 않는다. 자유게시판은 `community/free/` 에 있지만
주소는 `/free` 다. 어느 주소에 어느 화면이 붙는지는 `src/App.jsx` 만 정한다.

세 게시판이 **같이 쓰는 조각은 `community/components/` 에 둔다.** 지금은
`CommunityTabs` 하나다 — 레시피 추천 · 공지사항 · 자유게시판을 오가는 탭이고,
세 목록 화면이 똑같이 올려 쓴다. 사이드바에는 '커뮤니티' 하나만 있으므로
게시판 사이를 옮겨 다니는 길은 이 탭뿐이다. 하나를 지우면 그 게시판에
들어갈 방법이 없어진다.

```
community/
├── components/         세 게시판이 같이 쓰는 조각 (CommunityTabs)
├── recipeReview/
├── free/
└── notice/
```

---

## 폴더별로 무엇이 들어 있나

### `home/` — 대시보드

| 파일 | 주소 | 하는 일 |
|---|---|---|
| `HomePage.jsx` | `/` | 카드를 세로로 쌓아 보여준다 |

**다른 폴더의 컴포넌트를 많이 불러 쓴다.** 날씨 추천 · 시세 추천 · 미니게임 ·
예산 선택이 전부 여기 한 화면에 모인다. 카드 자체는
`home/components/` 에 있으니, 카드 안을 고칠 일이면 그쪽을 본다.

### `auth/` — 로그인 · 가입

| 파일 | 주소 | 하는 일 |
|---|---|---|
| `LoginPage.jsx` | `/login` | 로그인 |
| `RegisterPage.jsx` | `/register` | 회원가입 |
| `PasswordResetPage.jsx` | `/password-reset` | 인증번호 3단계 (발송 → 확인 → 교체) |

로그인한 사람(`user`)은 `App.jsx` 가 `useState` 로 들고 있다가 필요한 화면에
props 로 넘겨준다. 토큰과 사용자 정보를 localStorage 에 저장하는 건 `utils/token` 이 한다.

### `recommend/` — 추천 · 레시피

| 파일 | 주소 | 하는 일 |
|---|---|---|
| `RecommendPage.jsx` | `/recommend` | 조건으로 메뉴 고르기 |
| `BudgetPage.jsx` | `/budget` | 예산부터 정하고 고르기 |
| `SearchPage.jsx` | `/search` | 문장으로 찾기 |
| `RecipePage.jsx` | `/recipe/:foodName` | 레시피 + 재료 최저/최고가 |

`RecipePage` 가 가장 크다. 가격표는 `recommend/components/` 에 따로 있다.

### `game/` — 메뉴 정하기

| 파일 | 주소 | 하는 일 |
|---|---|---|
| `GamePage.jsx` | `/game` | 게임 고르는 화면 |
| `WorldCupPage.jsx` | `/game/worldcup` | 음식 월드컵 |
| `BalancePage.jsx` | `/game/balance` | 밸런스 게임 |
| `RoulettePage.jsx` | `/roulette` | 룰렛 |

게임에 나오는 후보와 질문은 코드에 박혀 있지 않다. 서버가 주는
`GET /api/catalog` 를 각 화면이 `useEffect` 로 직접 받아온다.

### `community/recipeReview/` — 레시피리뷰 · 인기 레시피

| 파일 | 주소 | 하는 일 |
|---|---|---|
| `ReviewListPage.jsx` | `/community` | 글 목록 · 검색 (공지 탭은 `/notice` 로 가는 링크) |
| `ReviewDetailPage.jsx` | `/community/:reviewId` | 글 + 댓글 + 좋아요 |
| `ReviewWritePage.jsx` | `/community/write` | 글쓰기 (로그인 필요) |
| `PopularPage.jsx` | `/popular` | 인기 레시피 |

조각은 `recipeReview/components/` 에 셋 있다 — `ReviewCard`(목록 한 줄) ·
`ReviewAttachment`(첨부 사진 한 장) · `CommentItem`(댓글 한 줄).
공지 조각(`NoticeList` · `NoticeForm`)은 `community/notice/` 로 옮겼다.

> **퍼블리싱까지만 돼 있다. 기능은 팀원들이 붙인다.**
>
> 화면은 `src/data/sampleBoard.js` 의 가데이터로 그려진다. 모양은 다 나오지만
> 입력 · 검증 · 저장은 **일부러 비워 뒀다.** 버튼을 눌러도 아무 일도 안 난다.
>
> 남아 있는 것은 **보기 전환뿐**이다 — 탭(레시피 추천 ↔ 공지사항), 공지 펼치기,
> 수정 폼 열고 닫기, 페이지 번호 눌렀을 때 색 바뀌기. 전부 `useState` 하나씩이다.
>
> 없는 것 — 입력칸 값 묶기(`value` + `onChange`), 입력값 검사, `alert` ·
> `window.confirm`, API 호출, 좋아요 숫자 증감, 글자 수 세기, 로딩 · 오류 상태.
> 입력칸은 `value` 대신 `defaultValue` 나 `placeholder` 만 쓴다.
>
> 무엇을 채워야 하는지는 **각 파일 맨 위 `TODO(게시판 담당)`**
> 에 항목으로 적혀 있다. 코드는 안 적어 뒀다. 받아오는 모양은 `CODE_STYLE.md` 2번,
> 서버가 어떻게 동작해야 하는지는 `docs/BOARD_SPEC.md` 를 본다.
>
> `api/reviews.js` 는 지금 아무 화면도 부르지 않는다. 함수 목록과 주소는 그대로
> 있으니 지우지 말고 채워서 쓴다 (서버도 아직 501 이다).
>
> `App.jsx` 의 `<Route>` 에서 `user={user}` 를 안 넘기고 있다. 로그인 여부로
> 버튼을 가리려면 그것부터 붙여야 한다.

### `community/free/` — 자유게시판

| 파일 | 주소 | 하는 일 |
|---|---|---|
| `FreeListPage.jsx` | `/free` | 글 목록 · 검색 · 페이징 |
| `FreeDetailPage.jsx` | `/free/:postId` | 글 + 첨부 사진 + 댓글 (내 글이면 수정 · 삭제) |
| `FreeWritePage.jsx` | `/free/write` | 글쓰기 (로그인 필요) |

조각은 `free/components/` 에 셋 — `FreePostCard`(목록 한 줄) ·
`FreeAttachment`(첨부 사진 한 장) · `FreeCommentItem`(댓글 한 줄).

`recipeReview/` 와 화면 모양이 거의 같지만 **글을 쓰는 조건이 없다.** 후기는 내
식사 기록(`MEAL_HISTORIES`)을 하나 골라야 쓸 수 있는데, 자유게시판은 로그인만
했으면 아무 주제나 쓴다. 표도 그렇게 만들었다 — `FREE_POSTS` 에는 부모 FK 가 없다
(`db/09_add_free_posts.sql`).

좋아요는 없다. 그 표(`FREE_POST_LIKES`)를 안 만들었다. 댓글은 있고, 표는
레시피리뷰와 **같이 쓴다** — `POST_COMMENTS` 의 `BOARD_TYPE='F'` 가 자유게시판
몫이다(`db/10_post_comments.sql`). 첨부 사진은 후기와 같이 **글 하나당 한 장**
이고 표의 `FILE_*` 컬럼에 들어간다.

> **여기만 서버까지 다 돼 있다.** 세 게시판 중 자유게시판만 실제 API 를 부른다.
> 목록 · 상세 · 글쓰기 · 수정 · 삭제 · 조회수 · 댓글 CRUD · 사진 업로드/교체/삭제가
> 전부 동작한다. 서버 코드는 `backend/.../api/free/` 에 있고, 테스트는
> `FreePostTest` 다.
>
> 그래서 **후기 · 공지를 붙일 때 본보기로 쓰면 된다.** 받아오는 모양
> (`useEffect` + `async/await` + `catch` 의 if/else), 오류 문구 꺼내는 법,
> 사진을 `FormData` 로 보내는 법이 `api/freePosts.js` 와 세 화면에 그대로 있다.
>
> 가데이터(`src/data/sampleFree.js`)는 **지웠다.** 서버가 다 붙어서 아무도
> 쓰지 않는데, 남겨두면 "이 게시판도 아직 퍼블리싱인가" 로 읽힌다. 서버 없이
> 화면만 보고 싶으면 백엔드를 켜는 쪽이 맞다.

### `community/notice/` — 공지사항

| 파일 | 주소 | 하는 일 |
|---|---|---|
| `NoticeListPage.jsx` | `/notice` | 공지 목록 (고정 공지가 위) |
| `NoticeDetailPage.jsx` | `/notice/:noticeId` | 공지 본문 + (관리자) 수정 · 삭제 |
| `NoticeWritePage.jsx` | `/notice/write` | 공지 작성 |

조각은 `community/notice/components/` 에 둘이다 — `NoticeCard`(목록 한 줄) ·
`NoticeForm`(작성 · 수정 폼). **폼은 작성 화면과 상세 화면이 같이 쓴다.**
`notice` props 를 넘기면 '공지 수정', 안 넘기면 '새 공지 작성' 모양이 된다.

원래 공지는 `recipeReview/ReviewListPage` 의 공지 탭 안에서 제목을 누르면 그 자리에서 펼쳐지는
아코디언이었다. 목록 · 상세 · 작성이 다 있는 게시판으로 바꾸면서 폴더를 따로
뺐다. 커뮤니티의 공지 탭은 `/notice` 로 보내는 링크로 남아 있다.

> **퍼블리싱까지만 돼 있다.** `src/data/sampleBoard.js` 의 `SAMPLE_NOTICES` ·
> `SAMPLE_NOTICE_DETAIL` 을 그린다. 남은 동작은 수정 폼 열고 닫기, 페이지 번호
> 색 바뀌기뿐이고 등록 · 수정 · 삭제는 아무 일도 하지 않는다. 할 일은 각 파일
> 맨 위 `TODO(공지 담당)` 에 있다.
>
> 관리자 전용 버튼(공지 작성 · 수정 · 삭제)이 **지금은 늘 보인다.** 가릴 때는
> `App.jsx` 에서 `user={user}` 를 받아 `user.is_admin` 으로 판단한다. 작성 화면은
> `loginOnly()` 처럼 `adminOnly()` 를 만들어 감싸는 것이 맞다.
>
> 서버는 `api/notices.js` 의 여섯 함수를 쓴다 — `getNotices` · `getNotice` ·
> `countNoticeView` · `createNotice` · `updateNotice` · `deleteNotice`. 아직 전부
> 501 이다. 상세 조회(`GET /api/notices/{noticeId}`)는 아코디언 시절에 없던
> 주소라 이번에 백엔드 컨트롤러에 같이 넣었다. 규칙은 `docs/BOARD_SPEC.md` 의
> 공지 표에 있다.
>
> 조회수는 상세를 받아올 때 자동으로 오르지 않는다. 화면이 `countNoticeView` 를
> 따로 불러야 한다 — 목록을 열 때마다 모든 공지의 조회수가 오르면 숫자가
> 의미를 잃기 때문에 서버가 그렇게 나눠 뒀다.

### `mypage/` — 내 정보

| 파일 | 주소 | 하는 일 |
|---|---|---|
| `MyPage.jsx` | `/mypage` | 내 정보 · 못 먹는 재료 · 알림 · 탈퇴 |
| `HistoryPage.jsx` | `/history` | 식사 기록 |
| `AiHistoryPage.jsx` | `/ai-history` | 추천받은 기록 (브라우저에만 저장) |
| `LikedPage.jsx` | `/liked` | 찜한 레시피 |
| `SettingsPage.jsx` | `/settings` | 테마 · 알림 |

`SettingsPage` 만 **로그인 없이** 쓸 수 있다. 브라우저에 저장되는 값이라서다.
나머지는 `App.jsx` 의 `loginOnly()` 로 감싸서 로그인 안 했으면 `/login` 으로 보낸다.

---

## 고칠 때 알아둘 것

코드 모양(화살표 함수, async/await, 폼 검사 등)은 `frontend/CODE_STYLE.md` 를 따른다.

### 상대경로는 폴더 깊이만큼 깊어진다

`../` 를 몇 개 쓰는지는 그 파일이 `src/` 에서 몇 칸 들어가 있는지로 정해진다.
헷갈리면 같은 폴더의 다른 파일을 열어 그대로 베끼면 된다.

```jsx
// pages/mypage/MyPage.jsx            — 화면
import Loading from '../../components/common/Loading'
import ProfileForm from './components/ProfileForm'     // 이 기능 전용은 언제나 './'

// pages/mypage/components/ProfileForm.jsx — 조각 (한 겹 더)
import { getLikes } from '../../../api/likes'

// pages/community/free/FreeListPage.jsx — 게시판은 community 아래라 또 한 겹
import Pagination from '../../../components/common/Pagination'

// pages/community/free/components/FreePostCard.jsx — 그 조각
import { formatDate } from '../../../../utils/format'
```

### 스타일은 `src/styles/index.css` 한 파일에 다 있다

원래는 화면마다 `.css` 를 옆에 뒀는데 파일이 45개까지 늘어서 하나로 합쳤다.
그래서 화면 파일에는 `import './XxxPage.css'` 가 없다.

고칠 곳은 Ctrl+F 로 찾는다. 파일 안이 `/* ----- XxxPage ----- */` 로 나뉘어 있다.
새 화면을 만들었으면 그 화면 이름으로 구역을 하나 만들어 아래쪽에 붙인다
(예: `/* ----- FreeListPage ----- */`).

- 여러 화면이 같이 쓰는 클래스(`.card` · `.btn` · `.muted` · `.tag` · `.empty` ·
  `.divided`)는 파일 맨 아래 **전역** 구역에 있다. **고치면 모든 화면이 같이 바뀐다.**
- 다른 기능의 클래스를 그대로 쓰지 않는다. 자유게시판이 후기의 `.review-card` 를
  같이 쓰면 한쪽을 고칠 때 다른 게시판이 같이 바뀐다 — `.free-card` 처럼 따로 둔다.
- 순서가 의미를 갖는다. 같은 힘(명시도)끼리 부딪히면 **나중에 쓴 쪽이 이긴다.**

### 화면을 새로 만들면

1. 맡은 폴더에 `XxxPage.jsx` 를 만든다
2. `src/App.jsx` 에 import 와 `<Route>` 를 추가한다 — **두 곳 다 고쳐야 한다**
3. 로그인이 필요하면 `element={loginOnly(<XxxPage />)}` 로 감싼다
4. 사이드바·하단 탭에도 보이게 하려면 `layouts/components/` 의 메뉴를 고친다
5. 스타일은 `src/styles/index.css` 아래쪽에 `/* ----- XxxPage ----- */` 구역을 만들어 적는다

### 화면끼리 직접 부르지 않는다

지금 `pages/` 안에서 서로를 import 하는 곳은 한 군데도 없다. 화면을 부르는 건
`App.jsx` 뿐이다. 이 규칙이 지켜져야 폴더를 옮겨도 남의 화면이 안 깨진다.

조각도 마찬가지다. **다른 기능 폴더의 `components/` 를 import 하지 않는다.**
두 기능 이상이 쓰게 되면 그 조각을 `src/components/common/` 으로 옮긴다.
