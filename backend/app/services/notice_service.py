"""공지사항 로직 (STEP 27).

! 아직 만들지 않았다. 담당자가 채운다.

여기에 만들 함수들
------------------
라우터(app/routers/notices.py)가 부를 함수다.

    list_notices(db, page, size)        목록 (고정 공지가 위, 그 다음 최신순)
    increase_view(db, notice_id)        조회수 +1
    create_notice(db, user, payload)    작성 (관리자만)
    update_notice(db, notice_id, payload)  수정
    delete_notice(db, notice_id)        삭제 (use_yn='N')

쓸 수 있는 것은 이미 준비돼 있다
-------------------------------
    테이블   notices (db/01_schema.sql)
    모델     app/models/notice.py
    스키마   app/schemas/notice.py

관리자 판단은 라우터가 한다
---------------------------
app/core/deps.py 의 get_admin_user 가 USERS.USER_LEVEL=1 을 확인한다.
여기서 다시 볼 필요가 없다.
"""
