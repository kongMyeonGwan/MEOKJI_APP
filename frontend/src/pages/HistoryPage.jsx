import { useState } from 'react'
import { Link } from 'react-router-dom'
import RecipeView from '../components/recipe/RecipeView'
import { MOCK_HISTORIES } from '../mocks'
import { formatDate, formatPrice } from '../utils/format'
import '../styles/pages/HistoryPage.css'

/**
 * 내가 확정한 식사 기록 (STEP 16).
 *
 * 각 행은 확정 당시의 스냅샷이다. AI가 나중에 다른 레시피를 만들어도
 * 여기 저장된 내용은 바뀌지 않는다.
 */
export default function HistoryPage() {
  // 펼쳐진 항목의 id. 목록에서 바로 레시피를 확인할 수 있게 한다.
  const [openId, setOpenId] = useState(null)

  const histories = MOCK_HISTORIES

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
