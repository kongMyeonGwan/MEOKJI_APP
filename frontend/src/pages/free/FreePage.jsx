import { useState } from 'react'
import { Link } from 'react-router-dom'
import FreePostCard from './components/FreePostCard'
import Pagination from '../../components/common/Pagination'
import Icon from '../../components/common/Icon'
import { SAMPLE_FREE_POSTS } from '../../data/sampleFree'

// 자유게시판 목록 (/free).
// 보는 것은 로그인 없이 되고, 글쓰기는 로그인해야 된다.
//
// 레시피 후기(/community)와 다른 점
//   후기는 내 식사 기록을 하나 골라야 글을 쓸 수 있지만, 여기는 그 조건이 없다.
//   그래서 탭도 없고 음식 이름 태그도 없다. 제목 · 내용 · 사진뿐이다.
//
// TODO(자유게시판 담당): 지금은 가데이터(SAMPLE_FREE_POSTS)를 그리는 퍼블리싱 상태다.
//   1. api/freePosts.js 를 만들고 getFreePosts(keyword, page) 를 넣는다
//   2. posts, totalPages 를 useState 로 바꾸고 useEffect 에서 받아온다
//      (받아오는 모양은 CODE_STYLE.md 2번의 async/await + try/catch 를 그대로 쓴다)
//   3. 검색은 서버에 keyword 를 넘기는 방식으로 바꾼다 (아래 filter 는 지운다)
//   4. src/data/sampleFree.js 를 지운다
export default function FreePage({ user }) {
  const [keyword, setKeyword] = useState('') // 검색어
  const [page, setPage] = useState(1)

  // 가데이터를 검색어로 걸러서 보여준다. 서버가 생기면 이 줄은 지우고
  // getFreePosts(keyword, page) 가 돌려준 목록을 그대로 그린다.
  const posts = SAMPLE_FREE_POSTS.filter((post) => post.title.includes(keyword) || post.content.includes(keyword))
  const totalPages = 1

  const changePage = (nextPage) => {
    setPage(nextPage)
    window.scrollTo(0, 0)
  }

  return (
    <div className="card">
      <div className="free__head">
        <h2 className="section-title" style={{ margin: 0 }}>
          자유게시판
        </h2>

        {/* 로그인했으면 글쓰기, 아니면 로그인 안내 */}
        {user ? (
          <Link to="/free/write" className="btn btn--sm">
            글쓰기
          </Link>
        ) : (
          <Link to="/login" className="muted free__hint">
            로그인하고 글쓰기 →
          </Link>
        )}
      </div>

      <p className="muted free__desc">
        <Icon name="chat" size={15} />
        주제 제한 없이 이야기를 나누는 공간입니다. 요리 이야기가 아니어도 괜찮아요.
      </p>

      <input
        type="search"
        className="free__search"
        value={keyword}
        onChange={(e) => setKeyword(e.target.value)}
        placeholder="제목이나 내용으로 검색"
      />

      {posts.length === 0 ? (
        <p className="empty">{keyword ? '검색 결과가 없습니다.' : '아직 글이 없습니다. 첫 글을 남겨보세요.'}</p>
      ) : (
        <>
          <div className="divided free__list">
            {posts.map((post) => (
              <FreePostCard key={post.id} post={post} />
            ))}
          </div>
          <Pagination page={page} totalPages={totalPages} onChange={changePage} />
        </>
      )}
    </div>
  )
}
