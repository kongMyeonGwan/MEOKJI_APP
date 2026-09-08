# 물음표 문법 두 개 — `?.` 와 `??`

프론트엔드 소스 곳곳에 물음표가 붙은 문법 두 개가 나온다.
학원 수업에서 안 다뤘을 수 있는데, **딱 두 개뿐이고 규칙도 단순하다.**
이것만 알면 프론트엔드 전체를 읽는 데 막히는 곳이 없다.

한 줄 요약:

> `?.` = "앞이 없으면 그냥 멈춰라"
> `??` = "앞이 없으면 대신 이걸 써라"

여기서 **"없다"는 `null` 과 `undefined` 두 가지만** 가리킨다.
`0` 이나 `''`(빈 문자열)은 "없는 것"이 아니다. 이 점이 중요하다.

---

## 1. `?.` — 옵셔널 체이닝

### 무슨 문제를 푸는가

서버에서 받은 데이터에 값이 없을 때가 있다.

```js
const user = null
console.log(user.nickname)   // TypeError! 화면이 통째로 하얘진다
```

리액트에서는 이 에러 하나로 **화면 전체가 안 그려진다.**
그래서 원래는 이렇게 방어했다.

```js
// 예전 방식
const nickname = user && user.nickname
```

`?.` 는 이걸 짧게 쓴 것이다.

```js
// 지금 방식 — 위와 결과가 같다
const nickname = user?.nickname
```

`user` 가 `null` 이나 `undefined` 면 **`.nickname` 을 읽지 않고 `undefined` 를 돌려준다.**
에러가 안 난다.

### 세 가지 모양

```js
obj?.prop        // 속성 읽기
arr?.[0]         // 배열 인덱스
fn?.()           // 함수 호출
```

프로젝트에서 실제로 쓰는 예를 보자.

```js
// components/recipe/RecipeView.jsx
{ingredients?.map((item) => ( ... ))}
```

재료 목록이 아직 안 왔으면 `ingredients` 가 `undefined` 다.
`?.` 가 없으면 `undefined.map is not a function` 으로 터진다.

```js
// components/recommendation/Roulette.jsx
window.matchMedia?.('(prefers-reduced-motion: reduce)')
```

`matchMedia` 는 아주 옛날 브라우저에는 없는 함수다.
`?.()` 는 "함수가 있으면 부르고, 없으면 그냥 넘어가라" 는 뜻이다.

### 이어서 쓸 수도 있다

```js
// api/client.js
error.config?.url?.endsWith('/api/auth/login')
```

`error.config` 가 없으면 거기서 멈추고, 있으면 `.url` 을 본다.
`url` 도 없으면 또 멈춘다. **중간 어디서든 없으면 전체가 `undefined` 다.**

`&&` 로 쓰면 이렇게 길어진다.

```js
error.config && error.config.url && error.config.url.endsWith('/api/auth/login')
```

---

## 2. `??` — 널 병합

### 무슨 문제를 푸는가

"값이 없으면 기본값을 쓴다"를 표현한다.

```js
// api/client.js
baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8000'
```

`.env` 에 주소가 적혀 있으면 그걸 쓰고, 없으면 `localhost:8000` 을 쓴다.

### `||` 와 뭐가 다른가

**이게 핵심이다.** 둘은 비슷해 보이지만 결과가 다를 때가 있다.

| 왼쪽 값 | `왼쪽 \|\| 기본값` | `왼쪽 ?? 기본값` |
|---|---|---|
| `null` | 기본값 | 기본값 |
| `undefined` | 기본값 | 기본값 |
| `0` | **기본값** | **`0`** |
| `''` (빈 문자열) | **기본값** | **`''`** |
| `false` | **기본값** | **`false`** |

`||` 는 `0` 과 빈 문자열도 "없는 것" 취급해서 기본값으로 바꿔버린다.
`??` 는 `null` / `undefined` 일 때만 바꾼다.

실제로 문제가 되는 자리를 보자.

```js
// pages/HistoryPage.jsx
totalCost += history.estimated_cost ?? 0
```

여기서 `||` 를 쓰면 어떻게 될까? 결과는 같다. `0 || 0` 은 `0` 이니까.

하지만 이런 자리라면 다르다.

```js
const count = data.likeCount ?? 0    // 좋아요 0개면 0 이 그대로 남는다
const count = data.likeCount || 0    // 이것도 우연히 0 이라 같다
```

```js
const nickname = user.nickname ?? '이름없음'   // 빈 문자열이면 '' 그대로
const nickname = user.nickname || '이름없음'   // 빈 문자열이면 '이름없음'
```

**"0 과 빈 문자열을 진짜 값으로 볼 것인가"** 가 갈림길이다.
헷갈리면 `??` 를 쓰면 된다. 의도가 더 정확하다.

---

## 3. 둘을 같이 쓰기

가장 많이 나오는 조합이다.

```js
// pages/MyPage.jsx
<dd>{user?.nickname ?? '-'}</dd>
```

읽는 순서대로 풀면 이렇다.

1. `user` 가 없으면 → `undefined`
2. `user` 는 있는데 `nickname` 이 없으면 → `undefined`
3. 어느 쪽이든 `undefined` 면 → `'-'` 를 쓴다

즉 **"사용자 정보가 아직 없거나 닉네임이 비어 있으면 `-` 를 보여줘라"** 다.
if 문으로 풀면 이만큼 길어진다.

```js
let nickname
if (user === null || user === undefined) {
  nickname = '-'
} else if (user.nickname === null || user.nickname === undefined) {
  nickname = '-'
} else {
  nickname = user.nickname
}
```

---

## 4. 왜 안 없앴나

이 프로젝트는 어려운 문법을 일부러 걷어냈다
(`reduce` · `useMemo` · 메서드 체인 등은 전부 `for` 문과 평범한 함수로 풀었다).

그런데 `?.` 와 `??` 는 남겼다. 이유는 세 가지다.

1. **없애면 오히려 길고 어려워진다.** 위 3번 예시처럼 if 문 8줄이 된다
2. **규칙이 단 하나다.** "null 이나 undefined 면" 이 전부다.
   `reduce` 처럼 누적값 · 콜백 순서를 따로 배울 게 없다
3. **안 쓰면 화면이 하얘진다.** 서버 응답이 늦거나 값이 비면 리액트는
   에러 하나로 화면 전체를 못 그린다. 방어는 어차피 해야 한다

---

## 5. 정리

```js
a?.b          // a 가 없으면 undefined, 있으면 a.b
a?.()         // a 가 함수면 호출, 아니면 undefined
a ?? b        // a 가 null/undefined 면 b, 아니면 a
a?.b ?? c     // a 나 a.b 가 없으면 c
```

`||` 와 `??` 의 차이 하나만 기억하면 된다.

> `||` 는 `0` 과 `''` 도 기본값으로 바꾼다. `??` 는 안 바꾼다.
