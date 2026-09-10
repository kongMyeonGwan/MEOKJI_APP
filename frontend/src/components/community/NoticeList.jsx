import { useState } from 'react'
import { countNoticeView } from '../../api/notices'
import { formatDate } from '../../utils/format'
import '../../styles/components/community/NoticeList.css'

/**
 * 공지사항 목록.
 *
 * 게시글(PostCard)과 달리 상세 페이지로 넘기지 않고 그 자리에서 펼친다.
 * 공지는 본문이 짧고 개수도 적어서, 페이지를 이동시키면 목록으로 돌아오는
 * 왕복이 더 번거롭다.
 *
 * 정렬은 서버(notice_service.list_notices)가 이미 해둔 것을 그대로 쓴다.
 * 고정 공지가 위로 와야 한다는 건 화면 취향이 아니라 공지의 규칙이라,
 * 화면이 여럿 생겨도 같은 순서여야 한다.
 */
export default function NoticeList({ notices }) {
  // 열어둔 공지의 id. 한 번에 하나만 열린다(아코디언).
  const [openId, setOpenId] = useState(null)

  // 이번 방문에서 조회수를 올린 공지들. 접었다 폈다를 반복해도 한 번만 센다.
  const [counted, setCounted] = useState(() => new Set())

  // 서버가 올려준 조회수. { [id]: 숫자 }
  const [views, setViews] = useState({})

  /** 제목 줄을 눌렀을 때. 펼치는 순간에만 조회수를 올린다. */
  async function toggle(notice) {
    const willOpen = openId !== notice.id
    setOpenId(willOpen ? notice.id : null)

    if (!willOpen || counted.has(notice.id)) return

    // 먼저 표시해둔다. 응답을 기다리는 사이에 두 번 누르면 두 번 올라간다.
    setCounted((prev) => new Set(prev).add(notice.id))

    try {
      const { view_count } = await countNoticeView(notice.id)
      setViews((prev) => ({ ...prev, [notice.id]: view_count }))
    } catch {
      // 조회수는 숫자 하나다. 못 올렸다고 공지 읽는 걸 막을 이유가 없다.
    }
  }

  if (notices.length === 0) {
    return <p className="empty">등록된 공지가 없습니다.</p>
  }

  return (
    <ul className="notice-list">
      {notices.map((notice) => {
        const open = openId === notice.id
        const viewCount = views[notice.id] ?? notice.view_count

        return (
          <li
            key={notice.id}
            className={`notice${notice.pinned ? ' notice--pinned' : ''}`}
          >
            {/* 제목 줄 전체가 토글 버튼이다. 클릭 영역이 넓어야 누르기 쉽다 */}
            <button
              type="button"
              className="notice__head"
              onClick={() => toggle(notice)}
              aria-expanded={open}
            >
              <span className="notice__badges">
                {notice.pinned && <span className="tag notice__pin">고정</span>}
                <span
                  className={`tag ${
                    notice.category === '점검'
                      ? 'notice__cat notice__cat--warn'
                      : 'notice__cat'
                  }`}
                >
                  {notice.category}
                </span>
              </span>

              <span className="notice__title">{notice.title}</span>

              <span className="muted notice__meta">
                {formatDate(notice.created_at)} · 조회 {viewCount}
              </span>

              {/* 회전하는 화살표로 열림/닫힘을 보여준다 */}
              <span className={`notice__caret${open ? ' is-open' : ''}`} aria-hidden="true">
                ⌄
              </span>
            </button>

            {open && (
              <div className="notice__body">
                <p className="notice__content">{notice.content}</p>
                <p className="muted notice__writer">— 뭐먹지? 운영팀</p>
              </div>
            )}
          </li>
        )
      })}
    </ul>
  )
}
