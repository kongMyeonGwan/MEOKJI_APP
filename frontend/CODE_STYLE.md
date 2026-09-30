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

## 1-1. 파일 하나에 컴포넌트 하나

화면(`pages/`)이 길어지면 조각을 `components/<폴더>/` 로 빼서 import 합니다.
예) 마이페이지의 폼들 → `components/mypage/ProfileForm.jsx`, `PasswordForm.jsx` ...

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
      setError(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }
  load()
}, [])
```

- API 함수는 `src/api/` 에 있습니다. 화면에서 axios 를 직접 부르지 않습니다.
- 에러 문구는 `getErrorMessage(err)` (`src/utils/apiError.js`) 로 꺼냅니다.

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
    setError(getErrorMessage(err))
  } finally {
    setSaving(false)
  }
}
```

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
