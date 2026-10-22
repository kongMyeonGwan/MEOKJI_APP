// 게시글에 붙은 사진 한 장. 후기 하나당 파일 하나라 갤러리가 아니다.
//
// 퍼블리싱만 된 조각이다. 사진은 가데이터고 버튼은 아직 아무 일도 하지 않는다.
//
// TODO(게시판 담당)
//   - 내 글일 때만(isMine) 바꾸기 · 지우기 버튼을 보이게 한다
//   - 파일을 고르면 uploadReviewFile, 지우기를 누르면 removeReviewFile 을 부른다
//   - 사진 주소는 api/reviews.js 의 reviewFileUrl(file.url) 로 만든다
//   - 올리는 중에는 버튼을 못 누르게 막고, 실패하면 오류 문구를 보여준다
export default function ReviewAttachment({ file }) {
  // 사진이 없는 글이면 이 칸을 그리지 않는다
  if (!file) {
    return null
  }

  return (
    <div className="attach">
      <figure className="attach__figure">
        <img className="attach__image" src={file.url} alt={file.name} />
        <figcaption className="muted attach__caption">
          <span className="attach__name">{file.name}</span>
          <span> · {file.sizeText}</span>
        </figcaption>
      </figure>

      <div className="attach__actions">
        {/* label 안에 숨긴 파일 입력칸을 넣으면 label 을 눌러도 파일 선택 창이 뜬다 */}
        <label className="btn btn--ghost btn--sm">
          사진 바꾸기
          <input type="file" accept=".jpg,.jpeg,.png,.gif,.webp" hidden />
        </label>

        <button type="button" className="btn btn--ghost btn--sm attach__danger">
          사진 지우기
        </button>
      </div>
    </div>
  )
}
