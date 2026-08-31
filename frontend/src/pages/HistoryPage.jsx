import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import RecipeView from '../components/recipe/RecipeView'
import { getHistories } from '../api/histories'
import { parseApiError } from '../utils/apiError'
import { formatDate, formatPrice } from '../utils/format'
import '../styles/pages/HistoryPage.css'

/**
 * 내가 확정한 식사 기록 (STEP 16).
 *
 * 각 행은 확정 당시의 스냅샷이다. AI가 나중에 다른 레시피를 만들어도
 * 여기 저장된 내용은 바뀌지 않는다.
 *
 * shared_post_id 가 있으면 이미 커뮤니티로 공유한 기록이다.
 * posts.meal_history_id 의 UNIQUE 제약 때문에 다시 공유할 수 없어서,
 * 배지를 띄우고 그 게시글로 바로 갈 수 있게 해둔다.
 */
export default function HistoryPage() {
  // 펼쳐진 항목의 id. 목록에서 바로 레시피를 확인할 수 있게 한다.
  const [openId, setOpenId] = useState(null)

  const [histories, setHistories] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  useEffect(() => {
    // 응답이 오기 전에 화면을 떠나면 setState가 헛돈다. 플래그로 막는다.
    let alive = true

    getHistories()
      .then((data) => {
        if (alive) setHistories(data)
      })
      .catch((err) => {
        if (alive) setLoadError(parseApiError(err).message)
      })
      .finally(() => {
        if (alive) setLoading(false)
      })

    return () => {
      alive = false
    }
  }, [])

  if (loading) {
    return (
      <div className="card">
        <h2 className="section-title">내 History</h2>
        <Loading message="기록을 불러오는 중..." />
      </div>
    )
  }

  if (loadError) {
    return (
      <div className="card">
        <h2 className="section-title">내 History</h2>
        <ErrorMessage message={loadError} />
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
          <Link to="/" className="history__link">오늘 먹을 음식을 찾아보세요 →</Link>
        </p>
      </div>
    )
  }

  const totalCost = histories.reduce((sum, h) => sum + (h.estimated_cost ?? 0), 0)

  return (
    <div className="history">
      <div className="card">
        <div className="history__head">
          <h2 className="section-title" style={{ margin: 0 }}>
            내 History
          </h2>
          <span className="muted history__summary">
            총 {histories.length}건 · 누적 {formatPrice(totalCost)}
          </span>
        </div>

        <ul className="history__list">
          {histories.map((item) => {
            const open = openId === item.id
            return (
              <li key={item.id} className="history__item">
                <button
                  type="button"
                  className="history__row"
                  onClick={() => setOpenId(open ? null : item.id)}
                  aria-expanded={open}
                >
                  <span className="history__date">{formatDate(item.selected_at)}</span>

                  <span className="history__names">
                    <strong>{item.food_name}</strong>
                    <span className="muted"> · {item.recipe_name}</span>
                  </span>

                  <span className="history__cost">{formatPrice(item.estimated_cost)}</span>
                  <span className="history__arrow">{open ? '▲' : '▼'}</span>
                </button>

                {open && (
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
                  </div>
                )}
              </li>
            )
          })}
        </ul>
      </div>
    </div>
  )
}
