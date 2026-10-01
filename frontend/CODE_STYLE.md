# 프론트엔드 코드 작성 규칙

새 화면이나 컴포넌트를 만들 때 이 모양을 따라 주세요. 기존 파일이 전부 이 규칙으로 쓰여 있으니, 비슷한 화면을 열어서 복사해 시작하면 됩니다.

## 1. 함수는 화살표 함수로

```jsx
// 컴포넌트는 function 으로 (파일마다 하나씩 export default)
export default function MyPage({ user }) {
  // 컴포넌트 안의 함수는 화살표 함수로
  const handleClick = () => {
    ...
  }
}
```

이벤트 함수 이름은 `handle~` 로 짓습니다. 예) `handleSubmit`, `handleDelete`, `handleLike`

함수는 `return` 을 생략하지 않고 `{ }` 안에 씁니다. 단, `map` / `filter` / `find` 안에 넣는 짧은 함수는 예외입니다.

```jsx
// ✅
const isUnread = (noti) => {
  return noti.created_at > readAt
}

// ✅ map · filter · find 안에서는 짧게 써도 된다
const names = items.map((item) => item.food_name)
```

## 1-1. 파일 하나에 컴포넌트 하나 · 기능 폴더에 모으기

스프링에서 기능별 패키지(`auth/controller · service · dao · vo`) 안에 그 기능 것만
두는 것처럼, 화면도 **기능 폴더 하나에 그 기능 것을 다 모읍니다.**

```
src/
├── components/common/     여러 기능이 같이 쓰는 것 (Modal, Loading, FoodCard ...)
├── pages/<기능>/
│   ├── components/        이 기능에서만 쓰는 조각 (+ 같은 이름의 .css)
│   ├── XxxPage.jsx        화면 (+ XxxPage.css)
├── layouts/               MainLayout + layouts/components (Sidebar, TopBar, BottomNav)
├── hooks/                 useForm
└── styles/                index.css (전체 색 · 공통 클래스), ListPage.css (여러 목록 화면이 같이 씀)
```

- 화면이 길어지면 조각을 `pages/<기능>/components/` 로 빼서 import 합니다.
  예) 마이페이지의 폼들 → `pages/mypage/components/ProfileForm.jsx`, `PasswordForm.jsx` ...
- **두 기능 이상이 쓰게 되면** `components/common/` 으로 옮깁니다.
  다른 기능 폴더(`pages/다른기능/components`)를 직접 import 하지 않습니다.
- CSS 는 같은 이름으로 바로 옆에 둡니다. `ProfileForm.jsx` ↔ `ProfileForm.css`

(예외: `components/common/FoodIllustration.jsx` 는 SVG 그림 모음이라 한 파일에 둡니다.)

## 2. 서버에서 데이터 받아오기 (async/await)

```jsx
const [items, setItems] = useState([])
const [loading, setLoading] = useState(true)
const [error, setError] = useState('')

useEffect(() => {
  const load = async () => {
    try {
      const data = await getSomething()
      setItems(data)
    } catch (err) {
      // 서버가 보낸 오류 문장을 그대로 보여준다.
      if (err.response) {
        // 입력값 검사에 걸리면 detail 이 배열로 온다. 예) [{ msg: '닉네임을 입력해주세요' }]
        const detail = err.response.data.detail
        if (Array.isArray(detail)) {
          setError(detail[0].msg)
        } else {
          setError(detail || '요청을 처리하지 못했습니다.')
        }
      } else if (err.request) {
        // 백엔드를 안 켰거나 30초가 지난 경우
        setError('서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.')
      } else {
        // 그 밖의 에러 (예: api/reviews.js 의 "게시판 기능은 아직 준비 중입니다.")
        setError(err.message)
      }
    } finally {
      setLoading(false)
    }
  }
  load()
}, [])
```

- API 함수는 `src/api/` 에 있습니다. 화면에서 axios 를 직접 부르지 않습니다.
- 에러 문구는 위 `catch` 안의 `if / else` 를 **그대로 복사해서** 꺼냅니다.
  (예전의 `getErrorMessage` · `utils/apiError.js` 는 지웠습니다. 문구를 바꿀 때는 모든 화면을 같이 고쳐야 합니다.)

## 3. 화면 그리기: 로딩 → 에러 → 빈 목록 → 목록

```jsx
{loading && <Loading message="불러오는 중..." />}
{error && <ErrorMessage message={error} />}
{!loading && !error && items.length === 0 && <p className="empty">아직 없어요.</p>}

{!loading && !error && items.length > 0 && (
  <ul>
    {items.map((item) => (
      <li key={item.id}>{item.name}</li>
    ))}
  </ul>
)}
```

## 4. 폼 검사: 에러 state 하나 + if 로 순서대로

```jsx
const handleSubmit = async (e) => {
  e.preventDefault() // 폼이 새로고침되는 걸 막는다

  if (!title.trim()) {
    setError('제목을 입력해주세요')
    return
  }

  setError('')
  setSaving(true)
  try {
    await createSomething({ title: title.trim() })
  } catch (err) {
    // 2번의 catch 안 if / else 를 그대로 넣는다
  } finally {
    setSaving(false)
  }
}
```

## 4-1. 입력칸이 여럿이면 useForm

입력칸마다 `useState` 를 따로 두지 않고 `src/hooks/useForm.js` 하나로 모읍니다.
입력칸의 `name` 이 `values` 의 키와 같아야 합니다. 체크박스도 같은 `handleChange` 로 됩니다.

```jsx
const { values, handleChange, reset } = useForm({ email: '', password: '' })

<input name="email" value={values.email} onChange={handleChange} />
```

칸마다 에러를 따로 보여줄 때는 `validate(values)` 가 `{ 칸이름: '문구' }` 를 돌려주게 하고,
`touched`(건드린 칸)일 때만 보여줍니다. 예) `pages/auth/RegisterPage.jsx`

같이 쓰는 부품

| 부품 | 쓰는 곳 |
|---|---|
| `common/PasswordInput` | 비밀번호 칸 + 보기/숨기기 버튼. `ref` 를 넘기면 포커스를 줄 수 있다 |
| `common/FormField` | 라벨 + 입력칸 + 에러 문구 틀 |
| `common/Modal` | 확인 창. `window.confirm` 대신 쓴다 (`{open && <Modal onClose={...}>...</Modal>}`) |
| `pages/auth/components/CodeInput` | 인증번호 칸. 숫자를 치면 다음 칸으로 넘어간다 (재설정 화면 전용) |

## 5. 자주 쓰는 문법

| 하고 싶은 것 | 이렇게 |
|---|---|
| 배열에 하나 추가 | `setList([...list, newItem])` |
| 배열에서 하나 빼기 | `setList(list.filter((item) => item.id !== id))` |
| 조건에 맞는 것 하나 찾기 | `list.find((item) => item.key === key)` |
| 객체 값 하나만 바꾸기 | `setUser({ ...user, nickname: '새이름' })` |
| 글자 안에 값 넣기 | `` `/community/${id}` `` |
| 둘 중 하나 고르기 | `liked ? '찜 해제' : '찜하기'` |
| 조건이 맞을 때만 보이기 | `{user && <p>{user.nickname}님</p>}` |

## 6. 로그인 정보

- 로그인한 사람(`user`)은 `App.jsx` 가 들고 있다가 **props 로** 넘겨줍니다.
  화면에서 `user` 가 필요하면 `App.jsx` 의 `<Route>` 에서 `user={user}` 를 넘겨주세요.
- 로그인해야만 열리는 화면은 `App.jsx` 에서 `loginOnly(<XxxPage />)` 로 감쌉니다.
- 깊은 곳의 컴포넌트(카드 등)에서 로그인 여부만 알면 될 때는 `getToken()` 으로 확인해도 됩니다.

## 7. 주석

- "이 코드가 **무엇을** 하는지" 를 한두 줄로 씁니다.
- 아직 안 만든 부분은 `// TODO(담당):` 로 남깁니다.
