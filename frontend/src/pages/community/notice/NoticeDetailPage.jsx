import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import NoticeForm from './components/NoticeForm'
import Modal from '../../../components/common/Modal'
import Loading from '../../../components/common/Loading'
import ErrorMessage from '../../../components/common/ErrorMessage'
import { countNoticeView, deleteNotice, getNotice } from '../../../api/notices'
import { formatDateTime } from '../../../utils/format'

// 공지 상세 (/notice/1).
// 보기는 누구나 되고, 수정 · 삭제는 관리자만 된다.
// 서버도 관리자인지 한 번 더 확인한다. 여기서 버튼을 숨기는 것은 화면 안내일 뿐이다.
export default function NoticeDetailPage({ user }) {
  const { noticeId } = useParams() // 주소의 :noticeId 부분
  const navigate = useNavigate()

  const [notice, setNotice] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('') // 공지를 못 불러왔을 때
  const [actionError, setActionError] = useState('') // 삭제가 실패했을 때

  // 수정 폼을 열었는지. 폼 자체는 작성 화면과 같은 NoticeForm 을 쓴다
  const [editing, setEditing] = useState(false)

  // 삭제 확인 창 (window.confirm 대신 Modal 을 쓴다)
  const [confirmOpen, setConfirmOpen] = useState(false)

  // 로그인하지 않았으면 user 가 null 이다
  const isAdmin = Boolean(user?.is_admin)

  // 서버가 보낸 오류 문장을 꺼낸다 (FreeDetailPage 와 같은 방식)
  const messageOf = (err) => {
    if (err.response) {
      const detail = err.response.data?.detail
      if (Array.isArray(detail)) {
        return detail[0].msg
      }
      return detail || '요청을 처리하지 못했습니다.'
    }
    if (err.request) {
      // 백엔드를 안 켰거나 30초가 지난 경우
      return '서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.'
    }
    return err.message
  }

  // 화면에 들어올 때 한 번: 공지를 받아오고 조회수를 올린다
  useEffect(() => {
    // 주소의 번호가 바뀌었을 때 늦게 도착한 옛 응답이 화면을 덮어쓰지 않게 한다
    let cancelled = false

    const load = async () => {
      setLoading(true)
      setEditing(false)
      setActionError('')
      try {
        const data = await getNotice(noticeId)
        if (cancelled) {
          return
        }
        setNotice(data)
        setError('')

        // 조회수는 여기서 따로 올린다. 목록을 열 때마다 오르면 숫자가 의미를 잃는다.
        // 실패해도 공지는 이미 보이니 화면을 막지 않는다.
        try {
          const count = await countNoticeView(noticeId)
          if (!cancelled) {
            setNotice({ ...data, view_count: count })
          }
        } catch (ignore) {
          // 조회수만 못 올린 것이라 넘어간다
        }
      } catch (err) {
        if (!cancelled) {
          setError(err.response?.status === 404 ? '공지를 찾을 수 없습니다.' : messageOf(err))
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }
    load()

    return () => {
      cancelled = true
    }
  }, [noticeId])

  // 수정 폼이 저장을 마치면 부른다. 서버가 돌려준 값으로 화면을 갈아끼운다
  const handleSaved = (saved) => {
    setNotice(saved)
    setEditing(false)
  }

  // 삭제. replace 를 쓰면 뒤로 가기를 눌러도 지워진 공지로 안 돌아온다
  const handleDelete = async () => {
    setConfirmOpen(false)
    setActionError('')
    try {
      await deleteNotice(noticeId)
      navigate('/notice', { replace: true })
    } catch (err) {
      setActionError(messageOf(err))
    }
  }

  if (loading) {
    return <Loading message="공지를 불러오는 중..." />
  }

  if (error) {
    return (
      <div className="notice-detail">
        <p className="muted notice-detail__back">
          <Link to="/notice">← 공지사항</Link>
        </p>
        <div className="card">
          <ErrorMessage message={error} />
        </div>
      </div>
    )
  }

  return (
    <div className="notice-detail">
      <p className="muted notice-detail__back">
        <Link to="/notice">← 공지사항</Link>
      </p>

      {editing ? (
        <div className="card">
          <NoticeForm notice={notice} onCancel={() => setEditing(false)} onSaved={handleSaved} />
        </div>
      ) : (
        <article className="card">
          <h2 className="notice-detail__title">
            {notice.pinned && <span className="tag notice__pin">고정</span>}
            {notice.title}
          </h2>

          <div className="notice-detail__meta">
            <span>뭐먹지? 운영팀</span>
            <span className="muted">{formatDateTime(notice.created_at)}</span>
            <span className="muted">조회 {notice.view_count}</span>
          </div>

          <p className="notice__content notice-detail__content">{notice.content}</p>

          <ErrorMessage message={actionError} />

          <div className="notice-detail__actions">
            <Link to="/notice" className="btn btn--ghost btn--sm">
              목록
            </Link>

            {/* 관리자에게만 보인다 */}
            {isAdmin && (
              <span className="notice-detail__admin">
                <button type="button" className="btn btn--ghost btn--sm" onClick={() => setEditing(true)}>
                  수정
                </button>
                <button
                  type="button"
                  className="btn btn--ghost btn--sm notice-detail__danger"
                  onClick={() => setConfirmOpen(true)}
                >
                  삭제
                </button>
              </span>
            )}
          </div>
        </article>
      )}

      {confirmOpen && (
        <Modal onClose={() => setConfirmOpen(false)}>
          <h3 className="modal__title">이 공지를 지울까요?</h3>
          <p className="muted">지운 공지는 목록과 상세에서 보이지 않게 됩니다.</p>
          <div className="modal__actions">
            <button type="button" className="btn btn--ghost btn--sm" onClick={() => setConfirmOpen(false)}>
              취소
            </button>
            <button type="button" className="btn btn--sm" onClick={handleDelete}>
              삭제하기
            </button>
          </div>
        </Modal>
      )}
    </div>
  )
}