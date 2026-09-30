import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import ErrorMessage from '../../components/common/ErrorMessage'
import Loading from '../../components/common/Loading'
import RecipeView from '../../components/recipe/RecipeView'
import Pagination from '../../components/common/Pagination'
import { getHistories, getHistoryStats, removeHistory } from '../../api/histories'
import { getErrorMessage } from '../../utils/apiError'
import { formatDate, formatPrice } from '../../utils/format'
import '../../styles/pages/HistoryPage.css'

// 내 식사 기록 (History). "이걸로 먹을래요" 를 누른 레시피가 쌓인다.
// 줄을 누르면 그 자리에서 레시피가 펼쳐진다.
export default function HistoryPage() {
  const [histories, setHistories] = useState([])
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)
  const [stats, setStats] = useState(null) // { count: 총 건수, total_cost: 누적 금액 }
  const [openId, setOpenId] = useState(null) // 펼쳐진 기록의 id
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // 지금 페이지의 기록 받아오기
  const loadHistories = async () => {
    try {
      const data = await getHistories({ page: page })
      setHistories(data.items)
      setTotalPages(data.total_pages)
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  // 요약(총 건수, 누적 금액) 받아오기
  const loadStats = async () => {
    try {
      const data = await getHistoryStats()
      setStats(data)
    } catch (err) {
      // 요약이 안 보일 뿐이다
    }
  }

  // 페이지가 바뀔 때마다 목록을 다시 받아온다
  useEffect(() => {
    loadHistories()
  }, [page])

  useEffect(() => {
    loadStats()
  }, [])

  const changePage = (nextPage) => {
    setPage(nextPage)
    window.scrollTo(0, 0)
  }

  // 줄을 누르면 펼치고, 다시 누르면 접는다
  const toggleOpen = (id) => {
    setOpenId(openId === id ? null : id)
  }

  // 기록 삭제
  const handleDelete = async (item) => {
    if (!window.confirm(`'${item.food_name}' 기록을 지울까요?`)) {
      return
    }

    try {
      await removeHistory(item.id)
      loadHistories()
      loadStats()
    } catch (err) {
      // 커뮤니티에 공유한 기록은 서버가 막는다
      alert(getErrorMessage(err))
    }
  }

  if (loading) {
    return (
      <div className="card">
        <h2 className="section-title">내 History</h2>
        <Loading message="기록을 불러오는 중..." />
      </div>
    )
  }

  if (error) {
    return (
      <div className="card">
        <h2 className="section-title">내 History</h2>
        <ErrorMessage message={error} />
      </div>
    )
  }

  if (histories.length === 0) {
    return (
      <div className="card">
        <h2 className="section-title">내 History</h2>
        <p className="empty">
          아직 기록이 없습니다.
          <br />
          <Link to="/" className="history__link">
            오늘 먹을 음식을 찾아보세요 →
          </Link>
        </p>
      </div>
    )
  }

  return (
    <div className="history">
      <div className="card">
        <div className="history__head">
          <h2 className="section-title" style={{ margin: 0 }}>
            내 History
          </h2>
          {stats && (
            <span className="muted history__summary">
              총 {stats.count}건 · 누적 {formatPrice(stats.total_cost)}
            </span>
          )}
        </div>

        <ul className="history__list">
          {histories.map((item) => (
            <li key={item.id} className="history__item">
              <button type="button" className="history__row" onClick={() => toggleOpen(item.id)}>
                <span className="history__date">{formatDate(item.selected_at)}</span>
                <span className="history__names">
                  <strong>{item.food_name}</strong>
                  <span className="muted"> · {item.recipe_name}</span>
                </span>
                <span className="history__cost">{formatPrice(item.estimated_cost)}</span>
                <span className="history__arrow">{openId === item.id ? '▲' : '▼'}</span>
              </button>

              {openId === item.id && (
                <div className="history__detail">
                  <RecipeView
                    recipe={item.recipe_json}
                    ingredients={item.ingredients_json}
                    substitutes={item.substitutes_json}
                  />

                  <p className="muted history__share">
                    {item.shared_post_id ? (
                      <Link to={`/community/${item.shared_post_id}`} className="history__link">
                        커뮤니티에 공유한 글 보기 →
                      </Link>
                    ) : (
                      <Link to="/community/write" className="history__link">
                        이 기록으로 글 쓰기 →
                      </Link>
                    )}
                  </p>

                  <div className="history__actions">
                    <Link to={`/recipe/${item.food_name}`} className="btn btn--ghost btn--sm">
                      이 메뉴 다시 보기
                    </Link>
                    <button
                      type="button"
                      className="btn btn--ghost btn--sm history__delete"
                      onClick={() => handleDelete(item)}
                    >
                      기록 삭제
                    </button>
                  </div>
                </div>
              )}
            </li>
          ))}
        </ul>

        <Pagination page={page} totalPages={totalPages} onChange={changePage} />
      </div>
    </div>
  )
}
