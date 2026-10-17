# 「지금 싼 재료로」 카드 학습 가이드

첫 화면에 뜨는 「지금 싼 재료로」 카드가 어디서 무엇을 받아 그려지는지 따라가는
문서다. 위쪽 재료 칩(`시금치 26%↓ 530원/100g`)과 아래쪽 메뉴 카드 4개가 각각
어디서 오는지를 나눠서 본다.

> **먼저 알아둘 것 — 메뉴 카드는 싼 재료로 고른 게 아니다.**
> 칩과 메뉴는 한 응답에 같이 실려 오지만, 서로 아무 관계가 없다.
> 자세한 것은 [4. 메뉴 카드는 어떻게 고르나](#4-메뉴-카드는-어떻게-고르나) 에 있다.

---

## 0. 먼저 — 어디에 흩어져 있나

### 프론트엔드

```
pages/home/
├── HomePage.jsx                  <BargainPick /> 을 넣는 곳
└── components/BargainPick.jsx    카드 전체 (칩 + 메뉴 + 출처 문구)   ★ 핵심

api/recommendations.js            getRecommendations()  POST /api/recommendations
styles/index.css                  /* ----- BargainPick ----- */  .bargain__*
```

### 백엔드

```
recommend/
├── controller/RecommendController.java    POST /api/recommendations
├── service/RecommendServiceImpl.java      use_bargains 가 켜지면 bargains 를 붙임
├── service/BargainServiceImpl.java        값이 내린 재료 찾기                ★ 핵심
└── vo/BargainVO.java                      칩 한 개 { name, percent, price, unit, surveyedAt }

util/
├── KamisClient.java                       KAMIS 시세 API 호출 + 일주일 등락률   ★ 핵심
├── IngredientMatcher.java                 재료 이름 → KAMIS 품목코드
└── FoodScorer.java                        메뉴 카드 고르기 (칩과 무관)

resources/data/kamis-items.json            재료 이름 ↔ 품목코드 표
resources/application.yml                  app.price.* 설정
data/bargains.json                         훑은 결과 캐시 파일 (git 에 안 올림)
```

**`BargainPick.jsx` · `BargainServiceImpl` · `KamisClient.priceTrend()` 셋을 읽으면 된다.**

---

## 1. 흐름을 끝까지 따라가기

```
1  HomePage.jsx                  <BargainPick /> 렌더
2  BargainPick.jsx useEffect     getRecommendations({ source: '시세 추천',
                                                      useBargains: true, limit: 4 })
3  api/recommendations.js        POST /api/recommendations  { use_bargains: true, limit: 4, ... }
4  RecommendController           findLoginUser(token)   로그인 안 했으면 null (막지 않음)
5  RecommendServiceImpl          ① foodScorer.recommend()   메뉴 4개    → items
                                 ② bargainService.current() 싼 재료    → bargains
                                 ③ makeCondition()          "시세 시금치 26%↓ 외 7건"
6  응답                           { condition, items, weather: null, bargains }
7  BargainPick.jsx               bargains → 칩,  items → 메뉴 카드
```

화면이 쓰는 응답 모양은 이렇다.

```json
{
  "condition": "시세 시금치 26%↓ 외 7건",
  "items":    [ { "food_name": "부추전", "reason": "부추 향이 살아있는 ...", ... } ],
  "weather":  null,
  "bargains": [ { "name": "시금치", "percent": -26.4, "price": 530,
                  "unit": "100g", "surveyed_at": "20261007" } ]
}
```

### 카드가 안 보일 때

`BargainPick` 은 세 경우에 카드를 **통째로 숨긴다** (`return null`).

| 경우 | 왜 생기나 |
|---|---|
| 요청이 실패했다 | `catch` 에서 아무것도 안 하고, `bargains` 가 빈 채로 남는다 |
| `bargains` 가 비었다 | 서버를 켠 직후라 아직 훑기가 안 끝났거나, 인증키가 없거나, 5% 넘게 내린 게 없다 |
| `items` 가 비었다 | 알레르기 때문에 후보가 다 빠진 경우 등 |

로딩 중에는 제목과 `"요즘 시세를 살펴보는 중..."` 만 보인다.

---

## 2. 싼 재료는 어떻게 찾나 — `BargainServiceImpl`

### 문제: 너무 느리다

KAMIS API 는 품목을 하나씩만 준다. 훑을 품목이 101개라 다 부르면 1분 가까이
걸린다. 첫 화면 요청이 1분을 기다릴 수는 없다.

### 해결: 요청은 바로 돌려주고, 훑기는 뒤에서 한다

`current()` 는 절대 기다리지 않는다.

```
current()
  1. 메모리에 결과가 없으면 → data/bargains.json 을 읽어본다 (오늘 날짜일 때만)
  2. 결과가 없거나 3시간이 지났으면 → startScan()  (새 스레드, 기다리지 않음)
  3. 지금 가진 것 중 pickCheap() 해서 바로 돌려준다  (없으면 빈 목록)
```

그래서 **서버를 켜고 첫 요청에는 칩이 안 나온다.** 1분쯤 지나 다시 들어오면 나온다.
`WARM_BARGAINS=true` 로 켜면 서버가 뜰 때(`ApplicationReadyEvent`) 미리 훑어둔다.

### 훑기 — `scan()`

```
for 품목 in ingredientMatcher.uniqueItemNames()     101개
    fetchOne(품목)
        IngredientMatcher.findCodes()   이름 → 품목코드
        kamisClient.fetch()             최대 3번 시도, 매번 150ms 쉼, 실패하면 2초 쉼
        → BargainVO { name, percent, price(최저가), unit, surveyedAt }

하나도 못 받았으면 → 예전 결과를 그대로 둔다 (통신이 막힌 날 빈 칸이 되지 않게)
percent 오름차순 정렬 (많이 내린 것이 앞)
lastRows 에 기억 + data/bargains.json 에 저장
```

**왜 116개가 아니라 101개인가?** `kamis-items.json` 에는 이름이 116개 있지만
그중에는 이름은 다르고 품목코드는 같은 것들이 있다. 같은 코드를
두 번 부를 필요는 없으니, `uniqueItemNames()` 가 코드마다 먼저 나온 이름 하나만
남긴다. 칩에 적히는 이름도 그 이름이다.

**오른 재료도 파일에는 남긴다.** `lastRows` 와 `bargains.json` 에는 오른 것까지
전부 들어 있다. 자르는 건 꺼내 쓰는 `pickCheap()` 이 한다. 나중에 "오늘은 배추가
비싸요" 같은 기능을 만들 때 다시 훑지 않아도 된다.

### 고르기 — `pickCheap()`

| 상수 | 값 | 뜻 |
|---|---|---|
| `MIN_DROP_PERCENT` | 5.0 | 5% 이상 내려야 "싸다" (`percent <= -5`) |
| `SHOW_COUNT` | 8 | 최대 8개 |
| `KEEP_MILLIS` | 3시간 | 이 시간이 지나면 다시 훑는다 |

### 파일 캐시 — `data/bargains.json`

```json
{ "scanned_on": "2026-10-07", "rows": [ { "name": "시금치", "percent": -26.4, ... } ] }
```

서버를 다시 켤 때마다 101번을 부르지 않으려고 둔 파일이다. **`scanned_on` 이 오늘이
아니면 버린다.** 어제 시세로 "지금 싸요" 라고 하면 안 되기 때문이다.
서버가 만드는 캐시라 `.gitignore` 에 들어 있다.

### 동시성 — `scanning` 과 `synchronized`

첫 화면에 여러 사람이 동시에 들어오면 `current()` 가 동시에 여러 번 불린다.
`startScan()` 이 `synchronized` 이고 `scanning` 플래그를 보기 때문에, 훑기는
한 번에 하나만 돈다. 스레드는 `setDaemon(true)` 라 서버를 끌 때 기다리지 않는다.

---

## 3. "일주일 전보다" 는 정확히 무엇과 무엇의 비교인가 — `KamisClient`

`KamisClient.fetch()` 는 품목 하나에 대해 **오늘부터 7일 전까지** 의 소매가격
(`se_cd = 01`)을 한 번에 받는다. 그 행들로 두 가지를 만든다.

```
history = sameUnitRows(rows)       단위를 하나로 맞춘다 (100g 과 1kg 섞이면 비교가 안 됨)
trend   = priceTrend(history)      등락률   → 칩의 "26%↓"
offers  = latestRows(history)      최신 조사일 가격들, 싼 순 → 칩의 "530원/100g"
```

`priceTrend()` 계산은 이렇다.

```
조사일별로 전국 시장 가격의 평균을 낸다
첫 조사일 평균(was) 과 마지막 조사일 평균(now) 을 비교
percent = (now - was) / was × 100
|percent| < 5 면 null  (흔들림 수준이라 알리지 않음)
```

주의할 점 두 가지.

- **"일주일 전" 은 정확히 7일 전이 아니다.** 7일 구간 안에서 *가장 이른 조사일* 과
  *가장 늦은 조사일* 을 비교한다. 주말 · 공휴일은 조사가 없어서 실제로는 4~6일
  차이일 수 있다.
- **등락률은 평균으로, 칩의 가격은 최저가로 계산한다.** `percent` 는 시장 평균끼리
  비교한 값이고, `price` 는 `offers.get(0)`, 즉 가장 싼 시장의 가격이다.

조회 결과는 `KamisClient` 안에서도 품목코드별로 3시간 캐시된다. 레시피 화면의
재료 가격(`PriceServiceImpl`)도 같은 `KamisClient` 를 쓰므로, 훑기를 한 번 하고
나면 레시피 화면의 가격 조회도 빨라진다.

---

## 4. 메뉴 카드는 어떻게 고르나

**싼 재료와 상관없이 고른다.** `RecommendServiceImpl.recommend()` 를 보면
`foodScorer.recommend(...)` 에 `bargains` 를 넘기지 않는다. 순서도 메뉴를 먼저
고르고 그 다음에 `bargainService.current()` 를 부른다.

이 카드의 요청은 검색어도, 날씨도, 카테고리도, 예산도 없다. 그러면
`FoodScorer.scoreFood()` 는 모든 메뉴에 0점을 준다. 정렬이
**점수 → `estimated_cost` 싼 순 → 이름순** 이라, 결국

> **카탈로그에서 예상 비용이 가장 싼 메뉴 4개** 가 나온다.

로그인했으면 알레르기 재료가 든 메뉴는 빠지고, 싫어하는 재료가 든 메뉴는 4점
깎여 뒤로 밀린다. 스크린샷의 부추전 · 애호박전 · 멸치볶음 · 열무비빔밥이 칩의
시금치 · 상추와 상관없어 보이는 이유가 이것이다.

### 왜 이렇게 됐나

원래는 이 재료 목록을 AI 프롬프트에 넣어 "이 재료를 쓰는 메뉴" 를 고르게 했다.
AI 호출을 걷어내면서 그 연결이 끊겼고, 지금은 칩과 `condition` 문구를 만드는 데만
쓴다. 자세한 경위는 [PROGRESS.md](PROGRESS.md) 의 "떨이 재료는 고른 메뉴에
영향을 주지 않는다" 에 있다.

`BargainPick.jsx` 맨 위 주석("그 재료로 만드는 메뉴(items)")은 그 전 기준이라
지금 동작과 다르다.

싼 재료를 실제로 메뉴 고르기에 반영하려면 새 기능을 만들어야 한다. 예를 들어
`FoodScorer` 에 `bargains` 를 받아 "그 재료를 쓰는 메뉴는 +N점" 규칙을 넣는 식이다.

---

## 5. 설정

`application.yml` 의 `app.price` 아래.

| 키 | 환경변수 | 기본값 | 뜻 |
|---|---|---|---|
| `service-key` | `DATA_GO_KR_SERVICE_KEY` | (비어 있음) | 공공데이터포털 **Decoding** 인증키. 비어 있으면 훑기를 아예 안 한다 |
| `warm-bargains` | `WARM_BARGAINS` | `false` | 서버가 뜰 때 미리 훑기 |
| `bargain-file` | `BARGAIN_FILE` | `data/bargains.json` | 결과 캐시 파일 |

**인증키가 없으면 카드가 영영 안 나온다.** 오류도 안 난다 — `startScan()` 이 조용히
돌아가고 `current()` 는 빈 목록만 준다.

---

## 6. 직접 확인해보기

1. **첫 요청에는 칩이 없는 것 보기** — 서버를 켠 폴더 기준 `data/bargains.json` 을 지우고 서버를
   다시 켠 뒤 첫 화면을 연다. 카드가 안 보인다. 콘솔에
   `[시세] 값이 내린 재료 N개를 찾았습니다` 가 찍힌 뒤 새로고침하면 나온다.
2. **파일 캐시 보기** — `bargains.json` 을 열어 오른 재료(`percent` 가 양수)도
   들어 있는지 본다. `scanned_on` 을 어제 날짜로 바꾸고 서버를 다시 켜면 파일을
   무시하고 다시 훑는다.
3. **기준 바꿔보기** — `MIN_DROP_PERCENT` 를 `15.0` 으로 올리면 칩이 몇 개로 줄어드나?
4. **메뉴가 무관한 것 확인하기** — 브라우저 개발자 도구에서 응답의 `items` 와
   `bargains` 를 나란히 본다. `items` 의 재료에 `bargains` 이름이 들어 있나?
5. **테스트 돌리기** — `BargainTest` 에 품목 101개 · 바로 돌려주기 ·
   `use_bargains` 켜고 끄기 테스트가 있다.

---

## 7. 스스로 답해보기

1. `current()` 가 훑기를 기다리지 않고 바로 돌려주는 이유는?
2. 서버를 켠 직후 카드가 안 보이는 건 버그인가? 어떻게 하면 바로 보이게 할 수 있나?
3. `bargains.json` 의 날짜가 어제면 버리는 이유는?
4. 칩의 `26%↓` 와 `530원` 은 각각 평균 · 최저가 중 무엇으로 계산했나?
5. 부추전이 이 카드에 나온 이유를 코드로 설명해보라.
6. 여러 사람이 동시에 첫 화면을 열어도 KAMIS 를 101번 × N 번 부르지 않는 이유는?

---

## 참고

| 파일 | 볼 곳 |
|---|---|
| [BargainPick.jsx](../frontend/src/pages/home/components/BargainPick.jsx) | 카드 전체 |
| [recommendations.js](../frontend/src/api/recommendations.js) | `getRecommendations()` |
| [RecommendServiceImpl.java](../backend/src/main/java/com/meokji/api/recommend/service/RecommendServiceImpl.java) | `recommend()` 6번 단계, `makeCondition()` |
| [BargainServiceImpl.java](../backend/src/main/java/com/meokji/api/recommend/service/BargainServiceImpl.java) | `current()` · `scan()` · `pickCheap()` |
| [KamisClient.java](../backend/src/main/java/com/meokji/api/util/KamisClient.java) | `fetch()` · `priceTrend()` |
| [FoodScorer.java](../backend/src/main/java/com/meokji/api/util/FoodScorer.java) | `recommend()` 정렬 순서 |
| [BargainTest.java](../backend/src/test/java/com/meokji/api/BargainTest.java) | 동작 확인 테스트 |
