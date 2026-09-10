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
 * 공지 목록. 고정(pinned) 공지가 위로, 그 안에서는 최신순으로 온다.
 *
 * 정렬은 서버가 한다. 고정이 위로 와야 한다는 건 화면 취향이 아니라
 * 공지의 규칙이라, 화면이 여럿 생겨도 같아야 하기 때문이다.
 *
 *   [{ id, title, content, category, view_count, pinned, created_at }, ...]
 */
export async function getNotices() {
  const { data } = await client.get('/api/notices')
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
