"""게시판 로직 (STEP 20~23).

! 아직 만들지 않았다. 담당자가 채운다.

여기에 만들 함수들
------------------
라우터(app/routers/posts.py)가 부를 함수다. 이름과 인자는 바꿔도 되지만,
라우터의 response_model 에 맞는 값을 돌려줘야 화면이 그대로 돈다.

    list_posts(db, keyword, page, size)            목록 + 검색 + 페이징
    list_popular(db, start, end, limit)            기간 내 좋아요 순
    get_post(db, post_id, current_user)            상세 + 댓글
    create_post(db, user, payload)                 글쓰기
    update_post(db, user, post_id, payload)        글 수정
    delete_post(db, user, post_id)                 글 삭제 (use_yn='N')
    increase_view(db, post_id)                     조회수 +1
    set_like(db, user, post_id, liked)             좋아요 켜기/끄기
    create_comment(db, user, post_id, payload)     댓글 작성
    update_comment(db, user, post_id, cid, payload) 댓글 수정
    delete_comment(db, user, post_id, cid)         댓글 삭제
    attach_file / detach_file / open_post_file     첨부파일

쓸 수 있는 것은 이미 준비돼 있다
-------------------------------
    테이블      recipe_reviews · review_comments · review_likes (db/01_schema.sql)
    모델        app/models/recipe_review.py · review_comment.py · review_like.py
    스키마      app/schemas/post.py  (요청/응답 모양이 다 정의돼 있다)
    파일 업로드 app/services/file_service.py  (저장 · 삭제 · 크기 표시)

규칙 두 가지
------------
    1. 삭제는 DELETE 가 아니라 use_yn='N' 이다. 그래서 모든 조회에
       use_yn='Y' 조건이 붙어야 한다.
    2. 후기는 자기 meal_histories 에서만 쓸 수 있다.
       recipe_reviews.meal_history_id 가 NOT NULL 이고,
       uq_reviews_meal 인덱스가 같은 기록을 두 번 공유하지 못하게 막는다.
"""
