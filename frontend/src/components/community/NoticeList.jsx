import { useState } from 'react'
import { countNoticeView } from '../../api/notices'
import { formatDate } from '../../utils/format'
import '../../styles/components/community/NoticeList.css'

// 공지사항 목록.
//
// 게시글이랑 다르게 상세 페이지로 안 가고 그 자리에서 펼친다.
// 공지는 본문이 짧고 개수도 적어서 페이지를 옮기면 오히려 번거롭다.
//
// 순서는 서버가 정해준 대로 그대로 쓴다.
//
// 관리자한테만 수정/삭제 버튼이 보인다(canManage).
// 버튼을 숨기는 건 잠그는 게 아니고 진짜 차단은 서버가 한다.
// 여기서 숨기는 건 눌러봐야 403 만 받을 버튼을 안 보여주려는 것이다.
export default function NoticeList({ notices, canManage, onEdit, onDelete }) {
  // 지금 열려 있는 공지의 id. 한 번에 하나만 열린다.
  const [openId, setOpenId] = useState(null)

  // 이번에 조회수를 이미 올린 공지 id 들.
  // 접었다 폈다 반복해도 한 번만 세려고 들고 있는다.
  const [counted, setCounted] = useState([])

  // 서버가 올려준 조회수. { 공지id: 숫자 }
  const [views, setViews] = useState({})

  // 제목 줄을 눌렀을 때. 펼치는 순간에만 조회수를 올린다.
  async function toggle(notice) {
    const willOpen = openId !== notice.id

    if (willOpen) {
      setOpenId(notice.id)
    } else {
      setOpenId(null)
    }

    if (!willOpen || counted.includes(notice.id)) {
      return
    }

    // 먼저 표시해둔다. 응답을 기다리는 사이에 두 번 누르면 두 번 올라간다.
    setCounted(counted.concat([notice.id]))

    try {
      const result = await countNoticeView(notice.id)

      setViews(function (prev) {
        const next = Object.assign({}, prev)
        next[notice.id] = result.view_count
        return next
      })
    } catch (err) {
      // 조회수는 숫자 하나다. 못 올렸다고 공지를 못 읽게 할 이유가 없다.
    }
  }

  // 삭제 버튼. 되돌리기 번거로워서 한 번 물어본다.
  function remove(notice) {
    const ok = window.confirm("'" + notice.title + "' 공지를 지울까요?")
    if (!ok) {
      return
    }

    onDelete(notice)
  }

  if (notices.length === 0) {
    return <p className="empty">등록된 공지가 없습니다.</p>
  }

  return (
    <ul className="notice-list">
      {notices.map(function (notice) {
        const open = openId === notice.id

        // 서버가 올려준 값이 있으면 그걸 쓰고 없으면 원래 값을 쓴다
        let viewCount = views[notice.id]
        if (viewCount === undefined) {
          viewCount = notice.view_count
        }

        let liClass = 'notice'
        if (notice.pinned) {
          liClass = 'notice notice--pinned'
        }

        let caretClass = 'notice__caret'
        if (open) {
          caretClass = 'notice__caret is-open'
        }

        return (
          <li key={notice.id} className={liClass}>
            {/* 제목 줄 전체가 버튼이다. 눌리는 영역이 넓어야 누르기 쉽다 */}
            <button
              type="button"
              className="notice__head"
              onClick={() => toggle(notice)}
            >
              {notice.pinned && (
                <span className="notice__badges">
                  <span className="tag notice__pin">고정</span>
                </span>
              )}

              <span className="notice__title">{notice.title}</span>

              <span className="muted notice__meta">
                {formatDate(notice.created_at)} · 조회 {viewCount}
              </span>

              {/* 화살표가 돌아가면서 열림/닫힘을 보여준다 */}
              <span className={caretClass} aria-hidden="true">
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
