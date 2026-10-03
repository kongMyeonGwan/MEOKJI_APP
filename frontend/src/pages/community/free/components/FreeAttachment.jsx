import { freePostFileUrl } from '../../../../api/freePosts'
import ErrorMessage from '../../../../components/common/ErrorMessage'

// 자유게시판 글에 붙은 사진 한 장.
// 글 하나당 파일 하나라(DB 의 FILE_* 컬럼) 갤러리가 아니다 — 본문 아래에 한 장만 놓인다.
//
// 내 글이면(isMine) 바꾸기 · 지우기 버튼이 보인다.
// 실제 업로드는 부모(FreeDetailPage)의 onPick, onRemove 에서 한다.
export default function FreeAttachment({ file, isMine, busy, error, onPick, onRemove }) {
  // 사진도 없고 내 글도 아니면 그릴 게 없다
  if (!file && !isMine) {
    return null
  }

  let pickLabel = file ? '사진 바꾸기' : '사진 붙이기'
  if (busy) {
    pickLabel = '올리는 중…'
  }

  return (
    <div className="free-attach">
      {file && (
        <figure className="free-attach__figure">
          <img className="free-attach__image" src={freePostFileUrl(file.url)} alt={file.name} />
          <figcaption className="muted free-attach__caption">
            <a
              href={freePostFileUrl(file.url)}
              target="_blank"
              rel="noreferrer"
              className="free-attach__name"
            >
              {file.name}
            </a>
            {file.size_text && <span> · {file.size_text}</span>}
          </figcaption>
        </figure>
      )}

      {isMine && (
        <div className="free-attach__actions">
          {/* label 안에 숨긴 파일 입력칸을 넣으면 label 을 눌러도 파일 선택 창이 뜬다 */}
          <label className="btn btn--ghost btn--sm">
            {pickLabel}
            <input
              type="file"
              accept=".jpg,.jpeg,.png,.gif,.webp"
              onChange={onPick}
              disabled={busy}
              hidden
            />
          </label>

          {file && (
            <button
              type="button"
              className="btn btn--ghost btn--sm free-attach__danger"
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
