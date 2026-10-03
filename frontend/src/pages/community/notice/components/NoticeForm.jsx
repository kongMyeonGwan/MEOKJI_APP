import { Link } from 'react-router-dom'

// 공지 쓰기 / 고치기 폼 (관리자만 보인다).
// notice 를 넘기면 고치기, 안 넘기면 새로 쓰기다.
// 작성 화면(NoticeWritePage)과 상세 화면의 수정 폼이 같이 쓴다.
//
// 퍼블리싱만 된 조각이다. 입력칸 모양만 있고 등록 · 수정은 아직 아무 일도 하지 않는다.
//
// TODO(공지 담당)
//   - 제목 · 내용이 비었는지 검사해서 칸 아래에 문구를 보여준다 (common/ErrorMessage)
//   - 입력값을 모은다 (hooks/useForm 을 쓰면 칸이 몇 개든 handleChange 하나로 된다)
//   - 새로 쓰기면 createNotice, 고치기면 updateNotice(notice.id) 를 부른다
//   - 저장이 끝나면 쓴 공지의 상세 화면으로 보낸다 (useNavigate)
export default function NoticeForm({ notice, cancelTo, onCancel }) {
  const editing = notice ? true : false

  return (
    <div className="notice-form">
      <h3 className="notice-form__title">{editing ? '공지 수정' : '새 공지 작성'}</h3>

      <label className="notice-form__field">
        <span>제목</span>
        <input
          type="text"
          defaultValue={editing ? notice.title : ''}
          placeholder="공지 제목을 입력해주세요"
          maxLength={200}
        />
      </label>

      <label className="notice-form__check">
        <input type="checkbox" defaultChecked={editing ? notice.pinned : false} />
        <span>목록 맨 위에 고정</span>
      </label>

      <label className="notice-form__field">
        <span>내용</span>
        <textarea
          defaultValue={editing ? notice.content : ''}
          placeholder="공지 내용을 입력해주세요"
          rows={10}
        />
      </label>

      <div className="notice-form__actions">
        {/* 취소는 두 가지다. 작성 화면이면 목록으로 나가고(cancelTo),
            상세 화면의 수정 폼이면 폼만 닫는다(onCancel) */}
        {cancelTo ? (
          <Link to={cancelTo} className="btn btn--ghost btn--sm">
            취소
          </Link>
        ) : (
          <button type="button" className="btn btn--ghost btn--sm" onClick={onCancel}>
            취소
          </button>
        )}

        <button type="button" className="btn btn--sm">
          {editing ? '수정' : '등록'}
        </button>
      </div>
    </div>
  )
}
