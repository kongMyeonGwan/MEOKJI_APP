import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import ErrorMessage from '../../components/common/ErrorMessage'
import Loading from '../../components/common/Loading'
import RecipeView from '../../components/common/RecipeView'
import Pagination from '../../components/common/Pagination'
import { getHistories, getHistoryStats, removeHistory } from '../../api/histories'
import { formatDate, formatPrice } from '../../utils/format'

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
      // 서버가 보낸 오류 문장을 그대로 보여준다.
      if (err.response) {
        // 입력값 검사에 걸리면 detail 이 배열로 온다. 예) [{ msg: '닉네임을 입력해주세요' }]
        const detail = err.response.data.detail
        if (Array.isArray(detail)) {
          setError(detail[0].msg)
        } else {
          setError(detail || '요청을 처리하지 못했습니다.')
        }
      } else if (err.request) {
        // 백엔드를 안 켰거나 30초가 지난 경우
        setError('서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.')
      } else {
        // 그 밖의 에러 (예: api/reviews.js 의 "게시판 기능은 아직 준비 중입니다.")
        setError(err.message)
      }
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
      // 서버가 보낸 오류 문장을 그대로 보여준다.
      if (err.response) {
        // 입력값 검사에 걸리면 detail 이 배열로 온다. 예) [{ msg: '닉네임을 입력해주세요' }]
        const detail = err.response.data.detail
        if (Array.isArray(detail)) {
          alert(detail[0].msg)
        } else {
          alert(detail || '요청을 처리하지 못했습니다.')
        }
      } else if (err.request) {
        // 백엔드를 안 켰거나 30초가 지난 경우
        alert('서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.')
      } else {
        // 그 밖의 에러 (예: api/reviews.js 의 "게시판 기능은 아직 준비 중입니다.")
        alert(err.message)
      }
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
                    {item.recipe_review_id ? (
                      <Link to={`/community/${item.recipe_review_id}`} className="history__link">
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
