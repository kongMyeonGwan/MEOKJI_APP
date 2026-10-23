import { reviewFileUrl } from '../../../../api/reviews'
import ErrorMessage from '../../../../components/common/ErrorMessage'

// 자유게시판 글에 붙은 사진 한 장.
// 글 하나당 파일 하나라(DB 의 FILE_* 컬럼) 갤러리가 아니다 — 본문 아래에 한 장만 놓인다.
//
//   file    : 붙어 있는 사진 { name, ext, size, size_text, url }. 없으면 null
//   isMine  : 이 글을 내가 썼는지. 버튼을 보일지 정한다
//   busy    : 올리는 중인지. 다 올라가기 전에 또 누르는 것을 막는다
//   error   : 올리다 실패했을 때 보여줄 문장
//   onPick / onRemove : 실제 업로드 · 삭제는 부모(ReviewDetailPage)가 한다
//
<<<<<<< HEAD
// isMine 은 글의 is_mine 이다 (부모가 post.is_mine 을 넘긴다).
// 사진은 글에 붙는 것이지 댓글에 붙는 것이 아니라서, 댓글의 is_mine 과는
// 아무 상관이 없다. 내 글이면 바꾸기 · 지우기가 보이고, 남의 글이면 사진만 보인다.
export default function ReviewAttachment({ file, isMine, busy, error, onPick, onRemove }) {
  // 사진도 없고 내 글도 아니면 그릴 게 없다.
  // (내 글이면 사진이 없어도 '사진 붙이기' 버튼은 보여야 하므로 둘 다 확인한다)
  if (!file && !isMine) {
=======
// TODO(게시판 담당)
//   - 내 글일 때만(isMine) 바꾸기 · 지우기 버튼을 보이게 한다
//   - 파일을 고르면 uploadReviewFile, 지우기를 누르면 removeReviewFile 을 부른다
//   - 사진 주소는 api/reviews.js 의 reviewFileUrl(file.url) 로 만든다
//   - 올리는 중에는 버튼을 못 누르게 막고, 실패하면 오류 문구를 보여준다
export default function ReviewAttachment({ file }) {
  // 사진이 없는 글이면 이 칸을 그리지 않는다
  if (!file) {
>>>>>>> develop
    return null
  }

  // 버튼 글자. 사진이 이미 있으면 '바꾸기', 없으면 '붙이기' 다.
  // 올리는 중에는 그 위에 덮어쓴다
  let pickLabel = file ? '사진 바꾸기' : '사진 붙이기'
  if (busy) {
    pickLabel = '올리는 중…'
  }

  return (
<<<<<<< HEAD
    <div className="free-attach">
      {/* 사진은 주인이 아니어도 보인다. 글을 볼 수 있으면 누구나 본다 */}
      {file && (
        <figure className="free-attach__figure">
          <img className="free-attach__image" src={reviewFileUrl(file.url)} alt={file.name} />
          <figcaption className="muted free-attach__caption">
            <a
              href={reviewFileUrl(file.url)}
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
=======
    <div className="attach">
      <figure className="attach__figure">
        <img className="attach__image" src={file.url} alt={file.name} />
        <figcaption className="muted attach__caption">
          <span className="attach__name">{file.name}</span>
          <span> · {file.sizeText}</span>
        </figcaption>
      </figure>
>>>>>>> develop

      {/* 버튼은 내 글일 때만. 숨기는 것은 보기 좋으라고 하는 것이고,
          남의 글에 사진을 올리려 해도 서버가 403 으로 막는다 */}
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
