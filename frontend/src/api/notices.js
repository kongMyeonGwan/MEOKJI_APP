import client from './client'

// 공지사항 API. 목록 보기는 로그인 없이도 된다.

// 공지 목록 한 페이지. 고정 공지가 위에, 그 다음은 최신순이다.
// 배열이 아니라 { items, total, page, size, total_pages } 가 온다.
export async function getNotices(options) {
  let page = 1
  let size = undefined

  if (options) {
    if (options.page) {
      page = options.page
    }
    size = options.size
  }

  const response = await client.get('/api/notices', {
    params: { page: page, size: size },
  })
  return response.data
}

// 조회수 +1. 올라간 값 { view_count } 가 온다.
// 목록을 불러올 때 같이 올리면 화면만 열어도 전부 올라가서 따로 뒀다.
export async function countNoticeView(noticeId) {
  const response = await client.post('/api/notices/' + noticeId + '/view')
  return response.data
}

// --- 아래는 관리자만 쓸 수 있다 ---
// 화면에서도 버튼을 숨기지만 진짜로 막는 건 서버다. 그냥 불러도 403 이 온다.

// 공지 작성. 작성자는 서버가 토큰에서 꺼내 넣는다.
export async function createNotice(form) {
  const response = await client.post('/api/notices', {
    title: form.title,
    content: form.content,
    pinned: form.pinned,
  })
  return response.data
}

// 공지 수정. PUT 이라서 세 항목을 전부 보낸다.
export async function updateNotice(noticeId, form) {
  const response = await client.put('/api/notices/' + noticeId, {
    title: form.title,
    content: form.content,
    pinned: form.pinned,
  })
  return response.data
}

// 공지 삭제. 서버는 진짜로 지우지 않고 use_yn 을 'N' 으로 바꾼다.
export async function deleteNotice(noticeId) {
  await client.delete('/api/notices/' + noticeId)
}
