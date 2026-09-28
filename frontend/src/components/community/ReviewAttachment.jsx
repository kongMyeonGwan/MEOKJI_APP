import { reviewFileUrl } from '../../api/reviews'
import ErrorMessage from '../common/ErrorMessage'
import '../../styles/components/community/ReviewAttachment.css'

// 게시글에 붙은 사진 한 장.
//
// 글 하나당 사진 하나다. 여러 장이 필요해지면 DB 부터 테이블을 나눠야 한다.
//
// img src 에 서버 주소가 붙는 이유는 업로드 폴더를 그냥 열어두지 않아서다.
// 그러면 지워진 글의 사진도 주소만 알면 계속 보인다.
// 대신 GET /api/reviews/{id}/file 이 use_yn 을 보고 내려준다.
//
// 내 글일 때만 바꾸기 / 지우기 버튼이 보인다.
//
// ! 사진 올리기·지우기는 아직 서버에 안 붙어 있다. 게시판 담당자가 채운다.
//   이 컴포넌트는 props 로 받은 것만 그린다. 실제 업로드는
//   부모(ReviewDetailPage)의 onPick · onRemove 에서 한다.
export default function ReviewAttachment({
  file,
  isMine,
  busy,
  error,
  inputRef,
  onPick,
  onRemove,
}) {
  // 사진도 없고 내 글도 아니면 그릴 게 없다
  if (!file && !isMine) {
    return null
  }

  // 사진 버튼 글자
  let pickLabel = '사진 붙이기'
  if (file) {
    pickLabel = '사진 바꾸기'
  }
  if (busy) {
    pickLabel = '올리는 중…'
  }

  let pickClass = 'btn btn--ghost btn--sm'
  if (busy) {
    pickClass = 'btn btn--ghost btn--sm is-busy'
  }

  return (
    <div className="attach">
      {file && (
        <figure className="attach__figure">
          {/* 서버가 이미지만 받아서 <img> 로 그려도 된다 */}
          <img
            className="attach__image"
            src={reviewFileUrl(file.url)}
            alt={file.name}
            loading="lazy"
          />
          <figcaption className="muted attach__caption">
            <a
              href={reviewFileUrl(file.url)}
              target="_blank"
              rel="noreferrer"
              className="attach__name"
            >
              {file.name}
            </a>
            {file.size_text && <span> · {file.size_text}</span>}
          </figcaption>
        </figure>
      )}

      {isMine && (
        <div className="attach__actions">
          {/* label 로 감싸면 기본 파일 선택 버튼을 숨기고
              우리가 만든 버튼 모양을 쓸 수 있다 */}
          <label className={pickClass}>
            {pickLabel}
            <input
              type="file"
              accept=".jpg,.jpeg,.png,.gif,.webp"
              ref={inputRef}
              onChange={onPick}
              disabled={busy}
              hidden
            />
          </label>

          {file && (
            <button
              type="button"
              className="btn btn--ghost btn--sm attach__danger"
              onClick={onRemove}
              disabled={busy}
            >
              사진 지우기
            </button>
          )}
        </div>
      )}

      <ErrorMessage message={error} />
    </div>
  )
}
