import { useState } from 'react'
import { formatDate } from '../../utils/format'
import '../../styles/components/community/NoticeList.css'

/**
 * 공지사항 목록.
 *
 * 게시글(PostCard)과 달리 상세 페이지로 넘기지 않고 그 자리에서 펼친다.
 * 공지는 본문이 짧고 개수도 적어서, 페이지를 이동시키면 목록으로 돌아오는
 * 왕복이 더 번거롭다.
 *
 * pinned 공지는 항상 위로 올린다. 그 안에서는 최신순이다.
 */
export default function NoticeList({ notices }) {
  // 열어둔 공지의 id. 한 번에 하나만 열린다(아코디언).
  const [openId, setOpenId] = useState(null)

  const sorted = [...notices].sort((a, b) => {
    if (a.pinned !== b.pinned) return a.pinned ? -1 : 1
    return new Date(b.created_at) - new Date(a.created_at)
  })

  if (sorted.length === 0) {
    return <p className="empty">등록된 공지가 없습니다.</p>
  }

  return (
    <ul className="notice-list">
      {sorted.map((notice) => {
        const open = openId === notice.id

        return (
          <li
            key={notice.id}
            className={`notice${notice.pinned ? ' notice--pinned' : ''}`}
          >
            {/* 제목 줄 전체가 토글 버튼이다. 클릭 영역이 넓어야 누르기 쉽다 */}
            <button
              type="button"
              className="notice__head"
              onClick={() => setOpenId(open ? null : notice.id)}
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
                {formatDate(notice.created_at)} · 조회 {notice.view_count}
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
