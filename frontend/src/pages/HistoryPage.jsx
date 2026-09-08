import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import RecipeView from '../components/recipe/RecipeView'
import { getHistories, removeHistory } from '../api/histories'
import { useProfile } from '../contexts/ProfileContext'
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
 * uq_reviews_meal(함수 기반 UNIQUE) 때문에 다시 공유할 수 없어서,
 * 배지를 띄우고 그 게시글로 바로 갈 수 있게 해둔다.
 *
 * 삭제도 여기서 한다. 공유한 기록은 서버가 409로 막는다.
 * 기록이 사라지면 게시글의 레시피가 통째로 비기 때문이다
 * (게시글은 레시피를 복사해 두지 않고 이 기록을 JOIN 해서 읽는다).
 */
export default function HistoryPage() {
  // 펼쳐진 항목의 id. 목록에서 바로 레시피를 확인할 수 있게 한다.
  const [openId, setOpenId] = useState(null)

  const [histories, setHistories] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  // 지우는 중인 항목의 id. 그 줄의 버튼만 잠근다.
  const [removingId, setRemovingId] = useState(null)
  const [actionError, setActionError] = useState('')

  const { reload } = useProfile()

  // 화면이 처음 열릴 때 내 기록을 불러온다.
  // 의존성이 [] 라서 딱 한 번만 실행된다.
  useEffect(() => {
    getHistories()
      .then((data) => setHistories(data))
      .catch((err) => setLoadError(parseApiError(err).message))
      .finally(() => setLoading(false))
  }, [])

  /** 삭제 버튼. 한 번 지우면 되돌릴 수 없어서 먼저 물어본다. */
  async function onRemove(item) {
    const ok = window.confirm(`'${item.food_name}' 기록을 지울까요?`)
    if (!ok) return

    setRemovingId(item.id)
    setActionError('')

    try {
      await removeHistory(item.id)

      // 낙관적으로 먼저 지우지 않는다. 실패했는데 화면에서만 사라지면
      // 새로고침할 때 되살아나 사용자가 혼란스럽다.
      setHistories(histories.filter((row) => row.id !== item.id))

      // 요리 횟수가 줄었으니 레벨 카드도 다시 맞춘다
      reload()
    } catch (err) {
      setActionError(parseApiError(err).message)
    } finally {
      setRemovingId(null)
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

  // 지금까지 먹은 것들의 예상 비용을 전부 더한다.
  // estimated_cost 가 없는(null) 기록도 있어서 ?? 0 으로 0 취급한다.
  let totalCost = 0
  for (const history of histories) {
    totalCost += history.estimated_cost ?? 0
  }

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

        <ErrorMessage message={actionError} />

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

                    <div className="history__actions">
                      <Link
                        to={`/recipe/${encodeURIComponent(item.food_name)}`}
                        className="btn btn--ghost btn--sm"
                      >
                        이 메뉴 다시 보기
                      </Link>
                      <button
                        type="button"
                        className="btn btn--ghost btn--sm history__delete"
                        onClick={() => onRemove(item)}
                        disabled={removingId === item.id}
                      >
                        {removingId === item.id ? '삭제 중…' : '기록 삭제'}
                      </button>
                    </div>
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
