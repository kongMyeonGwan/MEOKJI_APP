import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Icon from '../../components/common/Icon'
import FoodThumb from '../../components/common/FoodThumb'
import Loading from '../../components/common/Loading'
import LikedCard from './components/LikedCard'
import { getLikes, toggleLike } from '../../api/likes'

// 찜한 레시피 목록.
//
// 서버는 음식 이름(names)과 그 이름으로 카탈로그에서 찾은 정보(items)를 같이 준다.
// 화면은 items 를 그린다. 격자 보기와 목록 보기를 버튼으로 오간다.
export default function LikedPage() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [keyword, setKeyword] = useState('') // 검색창에 친 글자
  const [view, setView] = useState('grid') // 'grid'(격자) 또는 'list'(목록)

  // 찜 목록을 서버에서 받아온다
  const loadLikes = async () => {
    try {
      const data = await getLikes()
      setItems(data.items)
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
      await toggleLike(name)
      loadLikes()
    } catch (err) {
      alert('찜을 풀지 못했습니다.')
    }
  }

  // 검색창에 친 글자가 음식 이름에 들어 있는 것만 남긴다
  const shown = items.filter((item) => item.food_name.includes(keyword.trim()))

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
              onChange={(e) => setKeyword(e.target.value)}
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
          {shown.map((item) => (
            <LikedCard key={item.food_name} item={item} onUnlike={handleUnlike} />
          ))}
        </div>
      )}

      {!loading && shown.length > 0 && view === 'list' && (
        <div className="card">
          <ul className="liked">
            {shown.map((item) => (
              <li key={item.food_name}>
                <Link to={`/recipe/${item.food_name}`} className="liked__row">
                  <FoodThumb item={item} size="md" />
                  <span className="liked__body">
                    <span className="liked__name">{item.food_name}</span>
                    <span className="liked__meta muted">{item.reason || '눌러서 레시피를 만들어 보세요'}</span>
                  </span>
                </Link>

                <button
                  type="button"
                  className="liked__unlike"
                  onClick={() => handleUnlike(item.food_name)}
                  title="찜 해제"
                >
                  <Icon name="heart-fill" size={18} />
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
