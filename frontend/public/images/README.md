# 사진 두는 자리

여기 넣은 파일은 **주소 루트에서 바로 읽힌다.** `import` 하지 않는다.

```
frontend/public/images/LoginPage.jpg   →   <img src="/images/LoginPage.jpg">
```

지금 쓰는 파일

| 파일 | 쓰는 곳 |
|---|---|
| `LoginPage.jpg` | 로그인 화면 왼쪽 큰 사진 (`LoginPage`) |
| `game/chef.png` | 게임 배너 왼쪽 요리사 (`GamePromo` variant="full") |
| `game/chicken·pasta.png` | 음식 월드컵 카드의 VS 사진 |
| `game/stew·salad.png` | 취향 밸런스 카드의 VS 사진 |
| `game/roulette·ramen·burger.png` | 랜덤 돌림판 카드 |

`LoginPage.jpg` 가 없어도 화면은 정상으로 뜬다 — CSS 가 주황 그라데이션을 깔아두고
그 위에 사진을 얹는 구조라, 파일이 없으면 그라데이션만 보인다.

## 주의

- **원본 · 시안은 여기 두지 않는다.** `frontend/design/` 에 둔다. `public/` 안의 파일은
  쓰든 안 쓰든 전부 `dist/` 로 복사돼서, 원본을 두면 몇 MB 가 그대로 배포된다.
- **사용자가 올린 사진은 여기 두지 않는다.** 그건 서버의 `backend/data/uploads/`
  로 가고 `.gitignore` 에 들어 있다. 여기는 코드와 함께 배포되는 고정 이미지만 둔다.
- 파일이 커지면 첫 화면이 느려진다. 큰 사진은 1600px 안쪽, 300KB 안쪽으로 줄여서 넣는다.
- **사진은 PNG 가 아니라 JPEG 로 넣는다.** 같은 그림인데 크기가 열 배 넘게 차이난다
  (`LoginPage` 는 PNG 1,773KB -> JPEG 173KB 였다). PNG 는 로고 · 아이콘처럼
  색이 적고 경계가 또렷한 그림에 쓴다.
