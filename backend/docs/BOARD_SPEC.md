# 게시판 · 공지 명세 (담당자용)

`backend` 의 게시판 · 공지는 **501 스텁**이다. 주소 · 요청/응답 모양 · 권한은
다 맞춰져 있고 메서드 안만 채우면 된다.

자바는 **기능별 폴더**다. 기능 아래에 `controller/` · `service/`(인터페이스 +
`Impl`) · `dao/` · `vo/` 가 들어 있다. DTO 를 따로 두지 않는다 — VO 하나가
DB 한 행 · 화면이 보낸 요청 · 나갈 응답을 겸하고, 나가면 안 되는 칸에는
`@JsonIgnore` 를 붙인다. 게시판 담당 구역은 `review/` 와
`notice/` 폴더 통째이고, 채울 것은 `review/service/ReviewServiceImpl.java` ·
`review/dao/ReviewDAO.java` · `resources/mapper/ReviewMapper.xml` 이다.
나머지(컨트롤러 · 인터페이스 · DTO)는 이미 자리가 잡혀 있다.

이 문서는 **"무엇을 만들어야 하는가"** 가 아니라 **"어떻게 동작해야 하는가"** 를
적은 것이다. 앞의 것은 `ReviewService.java` · `NoticeService.java` 주석에 있다.

---

## 0. 먼저 — 돌아가던 구현이 git 에 있다

파이썬 쪽 게시판도 지금은 501 스텁이지만, **그 전에는 돌아갔다.** 화면을 가데이터로
바꾸면서(2026-09-18) 걷어낸 것이라 구현이 커밋에 남아 있다.

```
e22853f  2026-09-18  소스 단계 낮추기     <- 마지막으로 구현이 살아 있던 커밋
5576ddc  2026-09-18  가데이터             <- 여기서 501 스텁으로 바뀜
```

꺼내 보는 법:

```
git show e22853f:backend/app/services/post_service.py    # 620줄 — 게시판 로직 전부
git show e22853f:backend/app/services/notice_service.py  # 129줄 — 공지
git show e22853f:backend/app/services/file_service.py    # 135줄 — 첨부파일 저장
git show e22853f:backend/app/routers/posts.py            # 334줄
git show e22853f:backend/app/routers/notices.py          # 125줄
```

파일로 꺼내려면:

```
git show e22853f:backend/app/services/post_service.py > post_service.py
```

**이 저장소를 새로 파면 이게 사라진다.** 아래 내용은 그 구현에서 뽑아낸 것이지만,
막히면 원본을 직접 보는 편이 빠르다.

---

## 1. 지켜야 할 것 다섯 가지

### 1) 삭제는 `use_yn='N'` 이다

물리 삭제를 하지 않는다. **모든 조회에 `use_yn='Y'` 가 붙어야 한다.** 글 · 댓글 ·
좋아요 · 공지가 전부 그렇다. 빼먹으면 지운 글이 목록에 다시 나타나고, 지운 좋아요가
계속 세어진다.

### 2) 없는 것 · 남의 것 · 지운 것은 **전부 404** 다

403 을 쓰지 않는다.

```
없는 글 번호        -> 404
남이 쓴 글을 수정   -> 404   (403 이 아니다)
이미 지운 글        -> 404
다른 글의 댓글 번호 -> 404
```

남의 글일 때만 403 을 주면 **"그 번호는 존재한다"** 는 사실이 새어나간다. 문구도
하나로 통일한다 — `"게시글을 찾을 수 없습니다."` · `"댓글을 찾을 수 없습니다."`

### 3) 목록의 COUNT 와 본문은 **같은 조건 · 같은 JOIN** 을 봐야 한다

한쪽에만 검색어를 붙이면 *"3페이지가 있다는데 열면 비어 있는"* 일이 생긴다.
`HistoryMapper.xml` 의 `<sql id="mineWithShared">` 가 그 본보기다 — 조건을 한 번만
쓰고 COUNT 와 목록이 같이 `<include>` 한다.

### 4) 후기는 **내 식사 기록에서만** 쓸 수 있다

`recipe_reviews.meal_history_id` 가 NOT NULL 이고, `uq_reviews_meal` 인덱스가 같은
기록을 두 번 공유하지 못하게 막는다.

```
없는 기록 · 남의 기록 · 지운 기록  -> 404 "기록을 찾을 수 없습니다."
이미 후기를 쓴 기록                -> 409 "이미 공유한 기록입니다."
```

409 는 미리 조회해서 막지 말고 **`DuplicateKeyException` 을 잡아서** 낸다. 미리
조회한 뒤 INSERT 하기까지 사이에 다른 요청이 끼어들 수 있다. 오라클이 ORA-00001 을
던지며 메시지에 제약조건 이름(`UQ_REVIEWS_MEAL`)을 담아준다 —
`AuthService.register()` 가 이메일 중복에 쓰는 방식과 같다.

### 5) 첨부파일 컬럼 여섯 개는 **한 몸**이다

`file_yn` · `file_name` · `file_org_name` · `file_path` · `file_ext` · `file_size` 를
손으로 하나씩 만지면 안 된다. `ck_reviews_file_set` 이 ORA-02290 으로 막는다.
붙일 때 여섯 개를 같이 채우고, 뗄 때 같이 비운다.

---

## 2. 동작 명세

### 목록 `GET /api/reviews`

| 항목 | 규칙 |
|---|---|
| 정렬 | `created_at DESC, id DESC` — 두 번째 기준이 없으면 같은 글이 두 페이지에 나온다 |
| 검색 | 제목 **또는** 본문에 포함. `LOWER(col) LIKE LOWER(?)` |
| 검색어 이스케이프 | `\` `%` `_` 앞에 `\` 를 붙이고 `ESCAPE '\'` 를 준다. "50%" 로 검색하면 전체가 걸린다 |
| 본문 | **200자까지만** 잘라 보낸다. 화면은 CSS 로 2줄만 쓴다 |
| 좋아요 · 댓글 수 | 글마다 세지 말고 **이 페이지의 id 들을 한 번에** `GROUP BY` 로 센다 |
| `has_file` | `file_yn = 'Y'` 인지만. 경로 · 원본 이름은 상세에서만 쓴다 |
| 페이징 | `OFFSET ? ROWS FETCH NEXT ? ROWS ONLY` |

좋아요가 하나도 없는 글은 `GROUP BY` 결과에 아예 없다. **기본값 0** 으로 채운다.

### 상세 `GET /api/reviews/{id}`

- `foodName` · `recipeJson` · `ingredientsJson` 은 **복사본이 아니라 원본
  `meal_histories` 에서 읽는다.** 기록은 확정한 뒤로 수정하지 않으므로 값이 흔들리지 않는다.
- `liked` · `isMine` 은 로그인했을 때만 계산한다. 비로그인이면 둘 다 `false`
  (컨트롤러가 `authService.findLoginUser(token)` 으로 받아서 넘긴다 — 없으면 `null`).
- 댓글 표는 `POST_COMMENTS` 이고 **자유게시판과 같이 쓴다.** 모든 문장에
  `board_type = 'R'` 을 걸어야 한다 — 빠뜨리면 자유게시판 댓글이 섞여 나온다.
  (본보기: `resources/mapper/FreeCommentMapper.xml` 이 'F' 로 같은 일을 한다)
- 댓글은 `created_at, id` **오름차순** (목록과 반대다).
- 댓글의 `isMine` 은 "로그인했고 그 댓글을 내가 썼는가".

### 조회수 `POST /api/reviews/{id}/view`

상세와 나눠져 있다. 상세를 열 때마다 올리면 새로고침으로 조회수가 오른다.

**올린 뒤의 숫자를 그대로 돌려준다.** 감싸는 모양 없이 `12` 다 — 공지도
자유게시판도 같다. 돌려주는 이유는 화면이 1 을 더해 스스로 맞추면 다른
사람이 같은 시간에 연 것과 어긋나기 때문이다.

### 좋아요 `PUT/DELETE /api/reviews/{id}/like`

**껐던 행을 되살린다.** 새로 INSERT 하지 않는다.

```
켜달라 + 켜진 게 없음
    -> use_yn='N' 인 행이 있나 본다 (id DESC 로 하나)
       있으면  use_yn='Y' 로 되살린다
       없으면  새로 INSERT
끄달라 + 켜진 게 있음
    -> use_yn='N'
```

`uq_review_likes` 가 함수 기반 UNIQUE 라 `use_yn='Y'` 인 행만 중복을 막는다. 껐던 행을
남겨둔 채 새로 INSERT 하면 죽은 행이 계속 쌓인다.

동시에 두 번 눌러 INSERT 가 겹치면 `DuplicateKeyException` 이 온다. **삼킨다** —
결과는 "좋아요 켜짐" 으로 어차피 같다.

응답은 `{liked, like_count}` 이고, `like_count` 는 **바꾼 뒤 다시 센 값**이다.

### 인기 레시피 `GET /api/reviews/popular`

**기간 안에 새로 받은 좋아요** 기준이다. 글의 총 좋아요가 아니다.

| 항목 | 규칙 |
|---|---|
| 기본 기간 | 기간을 안 주면 **오늘 포함 최근 30일** |
| 날짜 경계 | **한국 날짜**다. `util/Kst` 로 UTC 범위로 바꿔서 비교한다 |
| 조건 | `review_likes.created_at >= 시작일 00:00 KST` 그리고 `< 종료일 다음날 00:00 KST` |
| 정렬 | 좋아요 많은 순 → **같으면 최근에 받은 순**(`MAX(created_at)`) |
| 썸네일 | DB 에 없다. `catalog.json` 에 같은 이름이 있으면 `art` · `image_url` 을 가져다 쓴다 |

두 번째 정렬 기준이 없으면 같은 데이터인데도 순서가 들쭉날쭉해 보인다.

`Kst` 를 안 쓰고 UTC 로 자르면 **9시간 때문에 경계에서 어긋난다** — 밤 9시 이후에
누른 좋아요가 다음 날로 넘어간다.

### 첨부파일 `POST/DELETE/GET /api/reviews/{id}/file`

**후기 하나당 파일 하나.** 다시 올리면 교체다.

저장 규칙:

```
폴더      uploads/yyyy/MM            (월별로 나눈다)
파일명    <UUID 32자>.<확장자>        원본 이름으로 저장하지 않는다
DB        원본 이름은 file_org_name 에 따로 적는다 (VARCHAR2(255) — 넘으면 자른다)
```

검사 순서:

1. **확장자 허용 목록** 대조 — 아니면 422
2. **받으면서 크기를 잰다.** 다 받아놓고 재지 않는다 — 넘는 순간 끊어야 디스크와
   메모리를 아낀다. 넘으면 413
3. 0바이트면 422
4. 실패하면 **쓰다 만 파일을 지운다**

경로 조작 막기: DB 의 `(폴더, 파일명)` 을 실제 경로로 바꾼 뒤, 그것이
**업로드 루트 아래인지 확인**한다. 아니면 404. `..%2F..%2Fetc%2Fpasswd` 같은 값이
DB 에 들어갔을 때를 막는 자리다.

순서가 중요하다:

```
붙일 때   옛 경로 기억 -> 새 파일 저장 -> DB 커밋 -> (실패하면 새 파일 삭제)
          -> 커밋 성공 후 옛 파일 삭제
뗄 때     DB 를 먼저 비우고 -> 그 다음 파일 삭제
글 삭제   파일은 디스크에서 지우지 않는다
```

반대로 하면 **DB 는 첨부가 있다는데 파일이 없는** 상태가 된다.

크기 표시(`size_text`)는 이렇게 만든다 — 화면이 그대로 띄운다.

```
1024 미만        "184 B"
1MB 미만         "184 KB"      소수점 없음
그 이상          "1.4 MB"      소수점 한 자리
```

내려줄 때는 원본 파일명을 `Content-Disposition` 에 담는다. CORS 설정에
`exposedHeaders` 로 이미 열어뒀다(`SecurityConfig`).

### 공지 `GET/POST/PUT/DELETE /api/notices`

| 항목 | 규칙 |
|---|---|
| 정렬 | `pinned_yn DESC, created_at DESC, id DESC` — `'Y' > 'N'` 이라 내림차순이면 고정이 위로 온다 |
| `pinned` | DB 는 `'Y'/'N'`, JSON 은 **`true`/`false`**. 양방향으로 바꿔줘야 한다 |
| 작성자 | **요청 본문이 아니라 토큰에서** 나온 사람이다. 본문으로 받으면 남의 이름으로 공지를 쓸 수 있다 |
| 권한 | 쓰기 · 수정 · 삭제는 `authService.getAdminUser(token)` — 관리자가 아니면 403. 목록과 조회수는 누구나 |
| 첨부 | **없다.** 표에 파일 컬럼이 없고 붙일 API 도 없다. 첨부는 후기에만 있다 |
| 상세 | `GET /api/notices/{noticeId}` — 없거나 `use_yn='N'` 이면 404. **조회수는 여기서 올리지 않는다** (화면이 `POST .../view` 를 따로 부른다) |
| 조회수 | `POST /api/notices/{noticeId}/view` — 올린 뒤의 숫자를 그대로 돌려준다 (`12`). 후기 · 자유게시판과 같다 |

---

## 3. 요청으로 받은 VO 를 그대로 DB 에 넣지 말 것

DTO 를 없애면서 **`RecipeReviewVO` · `ReviewCommentVO` · `NoticeVO` 가
`@RequestBody` 를 겸한다.** 표 한 행의 모든 칸이 그 클래스에 있으므로,
화면이 보낸 객체에는 **화면이 정할 권한이 없는 칸까지 들어올 수 있다.**

```
화면이 이렇게 보낼 수 있다
  POST /api/reviews
  { "meal_history_id": 5, "title": "제목", "content": "내용",
    "view_count": 99999, "user_id": 1, "file_yn": "Y" }
```

세 VO 모두 막아뒀다. 화면이 보내도 되는 칸만 열려 있다.

| VO | 열려 있는 칸 |
|---|---|
| `RecipeReviewVO` | `mealHistoryId` · `title` · `content` — 나머지는 전부 막혔다 |
| `ReviewCommentVO` | `content` |
| `NoticeVO` | `title` · `content` · `pinned` — `viewCount` 는 READ_ONLY 다 |

`@JsonIgnore` 는 내보낼 때뿐 아니라 **받을 때도 막는다.** 그래서 붙어 있는
칸은 화면이 아무리 보내도 null 로 남는다.

**받을 때만 막고 싶은 칸은 `@JsonIgnore` 가 아니다.** 닉네임 · 조회수 ·
댓글 수처럼 화면이 **봐야 하지만 정할 수는 없는** 칸은 `@JsonIgnore` 를
붙이면 응답에서도 사라진다. 이때는 이렇게 쓴다.

```java
@JsonProperty(access = JsonProperty.Access.READ_ONLY)
private Integer viewCount;        // 받을 때 무시, 보낼 때는 나간다
```

| 쓸 것 | 언제 |
|---|---|
| `@JsonIgnore` | 양쪽 다 막는다 — 글쓴이 id, 파일의 서버 경로 · UUID 이름, `useYn` |
| `READ_ONLY` | 받을 때만 막는다 — 닉네임, 조회수, 댓글 수, 내 글 여부 |

본보기는 `review/vo/RecipeReviewVO.java` 다. **목록 · 상세 응답까지 그 VO
하나로 겸한다** — 앞머리의 "VO 하나가
DB 한 행 · 요청 · 응답을 겸한다" 그대로다. 목록에 안 쓰는 칸(댓글 · 첨부 ·
레시피)은 목록 SQL 이 안 고르니 null 로 남을 뿐이다.

자유게시판(`free/`)은 이 방식이 아니다. JPA 로 옮기면서 요청과 응답을 DTO 로
나눴다 — `free/dto` 의 `FreePostRequest` · `FreePostListResponse` ·
`FreePostDetailResponse` 등. 받을 칸만 Request 에, 내보낼 칸만 Response 에
두므로 `@JsonIgnore` · `READ_ONLY` 로 막을 일이 없다.

**`request` 를 통째로 DAO 에 넘기면 안 된다.** 쓸 칸만 골라 새 VO 에 옮겨
담는다 — `BanServiceImpl.add()` 와 `HistoryServiceImpl.add()` 가 그렇게 한다.

```java
// 이렇게 (BanServiceImpl 방식)
UserBanVO ban = new UserBanVO();
ban.setUserId(user.getId());                        // 토큰에서 나온 값
ban.setIngredientName(request.getIngredientName()); // 화면이 정해도 되는 값만
banDAO.insert(ban);

// 이렇게 하면 안 된다
reviewDAO.insert(request);   // 조회수 · 작성자 · 첨부 여부까지 화면이 정하게 된다
```

조회수는 `POST /api/reviews/{id}/view` 에서만 올리고, 작성자는 언제나
토큰에서 나온 사람이다 — 위 「공지」 표의 **작성자** 항목과 같은 이유다.

첨부 여부(`fileYn`)와 파일 칸 다섯 개도 마찬가지다. 파일을 실제로 저장한
`attachFile()` 만 그 칸을 건드려야 한다 (1-5 의 「첨부파일 컬럼 여섯 개는
한 몸이다」 참고).

> `RecipeReviewVO` 의 주석에 어느 칸이 왜 막혔는지 적어뒀다. 칸을 하나
> 열어야 할 것 같으면 먼저 "서버가 이 값을 믿고 SQL 에 쓸 것인가" 를
> 보면 된다. 쓰지 않을 값이면 열 이유가 없다.

---

## 4. 만들 때 볼 것

| 무엇 | 어디 |
|---|---|
| 만들 메서드 목록 | `review/service/ReviewService.java` · `notice/service/NoticeService.java` 주석 |
| 주소 · 로그인 필요 여부 | `review/controller/ReviewController.java` · `notice/controller/NoticeController.java` |
| 요청/응답 모양 | `review/vo/` 4개 · `notice/vo/` 1개 (다 정의돼 있다) |
| 글 · 댓글 · 목록 · 상세 · 인기 | `RecipeReviewVO` · `ReviewCommentVO` — 표 한 행 · 요청 · 응답을 하나가 겸한다 |
| 좋아요 | `ReviewLikeVO` — 표 한 행이자 켜고 끈 결과다. `liked` 는 `use_yn` 에서 나온다 |
| 곁다리 응답 | `ReviewFileVO` — 첨부 한 건. `RecipeReviewVO.file` 안에 들어간다 |
| 표 한 행 | `review/vo/RecipeReviewVO.java` · `ReviewCommentVO` · `ReviewLikeVO` · `notice/vo/NoticeVO.java` |
| 매퍼 본보기 | `user/dao/UserDAO.java` + `resources/mapper/UserMapper.xml` |
| **페이징 본보기** | `resources/mapper/HistoryMapper.xml` 의 `<sql id="mineWithShared">` |
| **페이징 계산** | `common/Paging.java` — offset · total_pages 를 직접 계산하지 않는다. `HistoryServiceImpl.getList` 가 쓰는 모양을 그대로 따른다 |
| **되살리기 본보기** | `resources/mapper/LikeMapper.xml` — 찜하기가 좋아요와 같은 구조다 |
| 한국 날짜 | `util/Kst` |
| 오류 내는 법 | `common/ApiException` — `ResponseEntity` 로 직접 만들지 않는다 |
| 스키마 | `db/01_schema.sql` 의 `recipe_reviews` · `post_comments` · `review_likes` · `notices` |

**`resources/mapper/LikeMapper.xml` 을 먼저 보는 것을 권한다.** 찜하기(`recipe_likes`)가 게시글
좋아요(`review_likes`)와 같은 문제를 이미 푼 코드다 — 함수 기반 UNIQUE, 껐던 행
되살리기, 동시 요청 처리가 그대로 들어 있다.

---

## 5. 확인

`src/test/java/.../StubFixtureTest.java` 가 **주소 21개가 파이썬과 같은지** 검사한다.
구현하면서 주소를 바꿨다면 이 테스트도 같이 고쳐야 한다 — 프론트가 그 주소를 쓴다.

구현이 끝나면 데모 데이터가 저절로 채워진다:

```
gradlew seedDemo
```

지금은 501 이라 공지 4건과 후기 2건을 건너뛰는데, 구현되면 그 조건이 풀려
**파일을 고치지 않아도** 공지와 후기(첨부 포함)가 만들어진다.

마지막으로 화면을 바꾼다 — `frontend/src/pages/community/` 의 네 화면에서
`SAMPLE_*` 대신 `api/reviews.js` · `api/notices.js` 를 부르게 하고
`frontend/src/data/sampleBoard.js` 를 지운다.
