import { Link } from 'react-router-dom'
import { formatDate } from '../../../../utils/format'

// 자유게시판 목록의 한 줄. FreeListPage 가 글마다 하나씩 그린다.
// 레시피 후기와 달리 음식 이름 · 좋아요가 없어서 조회수와 댓글 수만 보여준다.
//
//   post : 목록 한 줄 { id, title, content, nickname, viewCount,
//                      commentCount, hasFile, createdAt }
//          content 는 본문 전체가 아니라 앞 200자다 (서버가 잘라서 보낸다).
//
// isMine 이 없다. 목록에는 수정 · 삭제 버튼이 없어서 내 글인지 알 필요가
// 없고, 그래서 서버 응답(FreePostListResponse)에 그 칸을 아예 넣지 않았다.
// 내 글 여부가 필요한 것은 글을 열고 난 다음(FreeDetailPage)이다.
//
//   listUrl : 지금 보고 있는 목록 주소. 예) '/free?page=3'
//             상세로 넘겨서 "목록" 버튼이 보던 페이지로 돌아오게 한다
export default function FreePostCard({ post, listUrl }) {
  return (
    <article className="free-card">
      {/* 제목과 미리보기를 누르면 상세로 간다. state 는 주소에 안 보이게 같이 넘기는 값이다 */}
      <Link to={`/free/${post.id}`} state={{ from: listUrl }} className="free-card__link">
        <h3 className="free-card__title">
          {post.title}
          {/* 첨부가 있으면 제목 옆에 클립만 붙인다. 사진 자체는 상세에서 보여준다 */}
          {post.hasFile && (
            <span className="free-card__clip" title="사진 있음">
              📎
            </span>
          )}
          {/* 댓글이 없으면 [0] 을 그리지 않고 아예 빼버린다 */}
          {post.commentCount > 0 && <span className="free-card__comments">[{post.commentCount}]</span>}
        </h3>
        {/* 본문 미리보기. 서버가 앞 200자만 잘라 보낸 값이다 */}
        <p className="muted free-card__excerpt">{post.content}</p>
      </Link>

      <footer className="free-card__meta">
        <span className="muted">{post.nickname}</span>
        <span className="muted">{formatDate(post.createdAt)}</span>
        <span className="muted free-card__counts">
          💬 {post.commentCount} · 조회 {post.viewCount}
        </span>
      </footer>
    </article>
  )
}
