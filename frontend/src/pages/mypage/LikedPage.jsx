import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Icon from '../../components/common/Icon'
import FoodThumb from '../../components/common/FoodThumb'
import Loading from '../../components/common/Loading'
import LikedCard from './components/LikedCard'
import Pagination from '../../components/common/Pagination'
import { getFavorites, toggleFavorite } from '../../api/favorites'

// 한 페이지에 보여줄 개수.
// 격자는 화면 폭에 따라 1~4열이 되는데, 12 는 2 · 3 · 4 어느 열로도 나누어떨어져 마지막 줄이 비지 않는다.
const PAGE_SIZE = 12

// 찜한 레시피 목록.
//
// 서버는 음식 이름(names)과 그 이름으로 카탈로그에서 찾은 정보(items)를 같이 준다.
// 화면은 items 를 그린다. 격자 보기와 목록 보기를 버튼으로 오간다.
//
// 페이징은 화면에서 한다. 서버는 찜 전체를 한 번에 주고(한 사람의 찜은 많아야 수백 개),
// 검색으로 거른 뒤 지금 페이지 몫만 잘라서 그린다.
export default function LikedPage() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [keyword, setKeyword] = useState('') // 검색창에 친 글자
  const [view, setView] = useState('grid') // 'grid'(격자) 또는 'list'(목록)
  const [page, setPage] = useState(1) // 지금 페이지 (1부터)

  // 찜 목록을 서버에서 받아온다
  const loadLikes = async () => {
    try {
      const data = await getFavorites()
      setItems(data.items || [])
    } catch (err) {
      alert('찜 목록을 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadLikes()
  }, [])

  // 하트를 누르면 찜을 풀고 목록을 다시 받아온다
  const handleUnlike = async (name) => {
    try {
      await toggleFavorite(name)
      loadLikes()
    } catch (err) {
      alert('찜을 풀지 못했습니다.')
    }
  }

  // 검색어가 바뀌면 1페이지부터 본다. 3페이지에서 검색하면 결과가 1페이지 분량뿐이라 빈 화면이 된다
  const changeKeyword = (value) => {
    setKeyword(value)
    setPage(1)
  }

  const changePage = (nextPage) => {
    setPage(nextPage)
    window.scrollTo(0, 0)
  }

  // 검색창에 친 글자가 음식 이름에 들어 있는 것만 남긴다
  const shown = items.filter((item) => item.foodName.includes(keyword.trim()))

  // 지금 페이지 몫만 잘라낸다.
  // 마지막 페이지의 마지막 하나를 찜 해제하면 그 페이지가 사라지므로, 남은 마지막 페이지로 당겨서 본다
  const totalPages = Math.ceil(shown.length / PAGE_SIZE)
  const currentPage = Math.min(page, Math.max(totalPages, 1))
  const pageItems = shown.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE)

  return (
    <div className="list-page list-page--wide">
      <header className="liked-head">
        <div>
          <h2 className="liked-head__title">❤️ 찜한 레시피</h2>
          <p className="muted liked-head__desc">다시 먹고 싶은 레시피를 한곳에 모아두었어요.</p>
        </div>

        {items.length > 0 && (
          <div className="liked-head__tools">
            <input
              type="search"
              className="liked-head__search"
              placeholder="찜한 레시피에서 검색하기..."
              value={keyword}
              onChange={(e) => changeKeyword(e.target.value)}
            />

            <div className="liked-head__views">
              <button
                type="button"
                className={view === 'grid' ? 'liked-head__view is-on' : 'liked-head__view'}
                onClick={() => setView('grid')}
                title="격자로 보기"
              >
                <Icon name="grid" size={18} />
              </button>
              <button
                type="button"
                className={view === 'list' ? 'liked-head__view is-on' : 'liked-head__view'}
                onClick={() => setView('list')}
                title="목록으로 보기"
              >
                <Icon name="list" size={18} />
              </button>
            </div>

            <span className="muted liked-head__count">총 {shown.length}개</span>
          </div>
        )}
      </header>

      {loading && <Loading message="찜한 레시피를 불러오는 중..." />}

      {!loading && items.length === 0 && (
        <p className="empty">
          아직 찜한 레시피가 없어요.
          <br />
          카드 오른쪽 위의 하트를 누르면 여기에 모입니다.
          <br />
          <br />
          <Link to="/recommend" className="btn btn--outline btn--sm">
            추천 메뉴 보러 가기
          </Link>
        </p>
      )}

      {/* 찜한 것은 있는데 검색어에 걸리는 게 없을 때 */}
      {!loading && items.length > 0 && shown.length === 0 && (
        <p className="empty">'{keyword}' 로 찾은 레시피가 없어요.</p>
      )}

      {!loading && shown.length > 0 && view === 'grid' && (
        <div className="liked-grid">
          {pageItems.map((item) => (
            <LikedCard key={item.foodName} item={item} onUnlike={handleUnlike} />
          ))}
        </div>
      )}

      {!loading && shown.length > 0 && view === 'list' && (
        <div className="card">
          <ul className="liked">
            {pageItems.map((item) => (
              <li key={item.foodName}>
                <Link to={`/recipe/${item.foodName}`} className="liked__row">
                  <FoodThumb item={item} size="md" />
                  <span className="liked__body">
                    <span className="liked__name">{item.foodName}</span>
                    <span className="liked__meta muted">{item.reason || '눌러서 레시피를 만들어 보세요'}</span>
                  </span>
                </Link>

                <button
                  type="button"
                  className="liked__unlike"
                  onClick={() => handleUnlike(item.foodName)}
                  title="찜 해제"
                >
                  <Icon name="heart-fill" size={18} />
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* 격자 · 목록 어느 보기든 같은 페이지를 유지한다. 한 페이지뿐이면 Pagination 이 알아서 숨는다 */}
      {!loading && <Pagination page={currentPage} totalPages={totalPages} onChange={changePage} />}
    </div>
  )
}
