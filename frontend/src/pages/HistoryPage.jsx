import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import RecipeView from '../components/recipe/RecipeView'
import Pagination from '../components/common/Pagination'
import { getHistories, getHistoryStats, removeHistory } from '../api/histories'
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
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)

  // 요약줄(총 N건 · 누적 M원)은 화면에 뜬 기록만 세면 안 된다.
  // 페이징이 붙은 뒤로 histories 는 한 페이지 몫뿐이라, 전체를 세는 건
  // 서버의 /api/histories/stats 가 한다.
  const [stats, setStats] = useState(null)

  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  // 지우는 중인 항목의 id. 그 줄의 버튼만 잠근다.
  const [removingId, setRemovingId] = useState(null)
  const [actionError, setActionError] = useState('')

  const { reload } = useProfile()

  // 페이지가 바뀔 때마다 그 페이지를 불러온다.
  //
  // alive 플래그가 필요한 이유는 게시판과 같다 — 2페이지를 눌렀다 3페이지를
  // 눌렀는데 2페이지 응답이 더 늦게 오면, 3페이지를 보면서 목록만 2페이지
  // 것으로 덮인다.
  useEffect(() => {
    let alive = true

    getHistories({ page })
      .then((data) => {
        if (!alive) return
        setHistories(data.items)
        setTotalPages(data.total_pages)
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
  }, [page])

  // 요약줄용 전체 집계. 목록과 따로 부른다.
  // 실패해도 요약줄만 비워둔다 — 기록을 못 보게 막을 이유가 없다.
  useEffect(() => {
    getHistoryStats()
      .then(setStats)
      .catch(() => {})
  }, [])

  /** 페이지 버튼. 목록이 바뀌므로 화면 맨 위로 올려준다. */
  function onPageChange(next) {
    setPage(next)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

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
      //
      // 화면에서 한 줄만 빼지 않고 그 페이지를 다시 받아온다. 한 건이 빠지면
      // 뒷 페이지의 첫 기록이 이 페이지로 당겨져 와야 하는데, 배열만 자르면
      // 19건짜리 페이지가 되고 그 기록은 어느 페이지에도 안 나온다.
      const data = await getHistories({ page })
      setHistories(data.items)
      setTotalPages(data.total_pages)

      // 마지막 기록을 지워 페이지가 통째로 사라졌으면 한 장 앞으로
      if (data.items.length === 0 && page > 1) setPage(page - 1)

      // 요약줄과 레벨 카드도 다시 맞춘다
      getHistoryStats().then(setStats).catch(() => {})
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

  if (histories.length === 0 && page === 1) {
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

  return (
    <div className="history">
      <div className="card">
        <div className="history__head">
          <h2 className="section-title" style={{ margin: 0 }}>
            내 History
          </h2>
          {/* 화면에 뜬 기록이 아니라 전체를 센 값이다 (GET /api/histories/stats).
              stats 가 아직 안 왔으면 자리만 비워둔다 */}
          {stats && (
            <span className="muted history__summary">
              총 {stats.count}건 · 누적 {formatPrice(stats.total_cost)}
            </span>
          )}
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

        {/* 한 페이지뿐이면 Pagination 이 스스로 아무것도 안 그린다 */}
        <Pagination page={page} totalPages={totalPages} onChange={onPageChange} />
      </div>
    </div>
  )
}
