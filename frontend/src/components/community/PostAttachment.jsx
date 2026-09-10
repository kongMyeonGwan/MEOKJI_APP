import { postFileUrl } from '../../api/posts'
import ErrorMessage from '../common/ErrorMessage'
import '../../styles/components/community/PostAttachment.css'

/**
 * 게시글에 붙은 사진 한 장 (STEP 26).
 *
 * 후기 하나당 파일 하나다. 목록이 아니라 한 장이라서 캐러셀도 그리드도 없다.
 * 여러 장이 필요해지면 DB 부터 REVIEW_FILES 테이블로 나눠야 한다
 * (db/01_schema.sql 의 FILE_* 컬럼 주석 참고).
 *
 * 왜 img src 에 서버 주소를 붙이나
 * --------------------------------
 * 업로드 폴더를 정적 서빙하지 않는다. 삭제된 글의 사진이 URL 만 알면
 * 계속 열리기 때문이다. 대신 GET /api/posts/{id}/file 이 use_yn 을 보고
 * 내려준다. 그래서 평범한 파일 주소가 아니라 API 주소가 들어간다.
 *
 * 내 글일 때만 아래에 바꾸기 · 지우기가 붙는다. 남의 글에서 눌러도
 * 서버가 404 로 막지만, 애초에 보여주지 않는다.
 */
export default function PostAttachment({
  file,
  isMine,
  busy,
  error,
  inputRef,
  onPick,
  onRemove,
}) {
  // 첨부도 없고 내 글도 아니면 그릴 것이 없다.
  // 남의 글에 "사진 없음" 을 적어봐야 읽을 이유가 없는 줄이다.
  if (!file && !isMine) return null

  return (
    <div className="attach">
      {file && (
        <figure className="attach__figure">
          {/* 서버가 이미지만 받지만, 설정을 바꿔 문서까지 받게 되면
              여기가 깨진 이미지가 된다. 그때는 확장자로 갈라야 한다. */}
          <img
            className="attach__image"
            src={postFileUrl(file.url)}
            alt={file.name}
            loading="lazy"
          />
          <figcaption className="muted attach__caption">
            <a
              href={postFileUrl(file.url)}
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
          {/* label 로 감싸면 못생긴 기본 파일 버튼을 숨기고
              우리 버튼 모양을 그대로 쓸 수 있다 */}
          <label className={`btn btn--ghost btn--sm ${busy ? 'is-busy' : ''}`}>
            {busy ? '올리는 중…' : file ? '사진 바꾸기' : '사진 붙이기'}
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
