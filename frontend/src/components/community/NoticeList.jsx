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
 *
 * 관리자에게만 수정·삭제 버튼이 붙는다(canManage). 버튼을 숨기는 것은 잠그는
 * 것이 아니다 — 진짜 차단은 서버의 get_admin_user 가 한다. 여기서 숨기는 건
 * 눌러봐야 403 만 받을 버튼을 보여주지 않기 위해서다.
 *
 * 버튼을 제목 줄이 아니라 펼친 본문 아래에 두는 이유가 둘 있다.
 *   - 제목 줄 전체가 이미 <button>(아코디언 토글)이다. 그 안에 버튼을 또
 *     넣으면 HTML 상 중첩 버튼이 되어 브라우저마다 다르게 동작한다
 *   - 삭제는 되돌리기 번거로운 동작이라, 내용을 펼쳐 확인한 뒤에 누르는
 *     자리에 있는 편이 낫다
 */
export default function NoticeList({ notices, canManage = false, onEdit, onDelete }) {
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

  /** 삭제 버튼. 되돌리기 번거로운 동작이라 한 번 묻는다. */
  function remove(notice) {
    const ok = window.confirm(`'${notice.title}' 공지를 지울까요?`)
    if (!ok) return

    onDelete(notice)
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
              {/* 배지는 '고정' 하나뿐이다. 분류(안내·점검·업데이트)는 없앴다 —
                  공지는 공지 하나로 끝낸다 */}
              {notice.pinned && (
                <span className="notice__badges">
                  <span className="tag notice__pin">고정</span>
                </span>
              )}

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

                {canManage && (
                  <div className="notice__admin">
                    <button
                      type="button"
                      className="btn btn--ghost btn--sm"
                      onClick={() => onEdit(notice)}
                    >
                      수정
                    </button>
                    <button
                      type="button"
                      className="btn btn--ghost btn--sm"
                      onClick={() => remove(notice)}
                    >
                      삭제
                    </button>
                  </div>
                )}
              </div>
            )}
          </li>
        )
      })}
    </ul>
  )
}
