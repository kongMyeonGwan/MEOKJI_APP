import client from './client'

/**
 * 공지사항 API.
 *
 * 예전에는 /api/catalog/notices 였다. catalog 는 카테고리 칩·룰렛 조각처럼
 * "사용자가 만들지 않는 고정 목록"을 파일에서 내려주는 곳인데, 공지는
 * 운영하면서 계속 새로 쓰는 값이라 성격이 다르다. 파일에 두면 공지 하나
 * 올릴 때마다 서버를 다시 배포해야 한다.
 *
 * 그래서 NOTICES 테이블로 옮겼고, 주소도 /api/notices 로 옮겼다.
 *
 * 로그인 없이 볼 수 있다. 공지는 Guest도 읽어야 한다.
 */

/**
 * 공지 목록 한 페이지. 고정(pinned) 공지가 위로, 그 안에서는 최신순으로 온다.
 *
 * 정렬은 서버가 한다. 고정이 위로 와야 한다는 건 화면 취향이 아니라
 * 공지의 규칙이라, 화면이 여럿 생겨도 같아야 하기 때문이다.
 *
 * 공지 배열이 아니라 이런 모양이 온다.
 *
 *     { items: [...], total: 4, page: 1, size: 20, total_pages: 1 }
 *
 * 고정 공지는 정렬상 앞쪽에 몰리므로 1페이지에만 보인다. 페이지마다 따라
 * 다니면 2페이지의 절반이 이미 읽은 공지가 된다.
 */
export async function getNotices({ page = 1, size } = {}) {
  const { data } = await client.get('/api/notices', {
    params: { page, size },
  })
  return data
}

/**
 * 조회수 올리기. 올라간 값 { view_count }를 돌려준다.
 *
 * 목록을 GET 할 때 서버가 같이 올리지 않는다. GET은 몇 번을 보내도 결과가
 * 같아야 하는데, 화면을 열 때마다 목록의 공지가 전부 1씩 오르면 숫자가
 * 뜻을 잃는다. 그래서 공지를 펼칠 때 화면이 이 함수를 부른다.
 */
export async function countNoticeView(noticeId) {
  const { data } = await client.post(`/api/notices/${noticeId}/view`)
  return data
}

// --- 아래는 관리자(user_level=1)만 쓸 수 있다 ---
//
// 화면은 user.is_admin 을 보고 버튼을 숨기지만, 그건 잠그는 게 아니라 가리는
// 것이다. 진짜 차단은 서버의 get_admin_user 가 한다 — 여기 함수를 콘솔에서
// 직접 불러도 403 이 온다.

/**
 * 공지 작성. 만들어진 공지 한 건을 돌려준다.
 *
 * 작성자와 조회수는 보내지 않는다. 작성자는 서버가 토큰에서 꺼내 넣고,
 * 조회수는 읽혀서 쌓이는 값이라 0부터 시작한다.
 */
export async function createNotice({ title, content, pinned }) {
  const { data } = await client.post('/api/notices', {
    title,
    content,
    pinned,
  })
  return data
}

/**
 * 공지 수정. 고쳐진 공지 한 건을 돌려준다.
 *
 * PATCH가 아니라 PUT이라 세 항목을 전부 보낸다. 화면이 폼에 기존 값을
 * 채워놓고 고치게 하므로 어차피 넷을 다 들고 있다.
 *
 * 조회수는 안 바뀐다. 오타 하나 고쳤다고 "몇 명이 읽었나"가 0으로
 * 돌아가면 안 되기 때문이다.
 */
export async function updateNotice(noticeId, { title, content, pinned }) {
  const { data } = await client.put(`/api/notices/${noticeId}`, {
    title,
    content,
    pinned,
  })
  return data
}

/**
 * 공지 삭제. 돌려주는 값이 없다(204).
 *
 * 서버는 행을 지우지 않고 use_yn='N'으로 바꾼다. 잘못 지웠으면 DB에서
 * 한 줄 되돌리면 조회수까지 그대로 살아난다.
 */
export async function deleteNotice(noticeId) {
  await client.delete(`/api/notices/${noticeId}`)
}
