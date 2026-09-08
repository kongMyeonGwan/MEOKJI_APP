import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import ErrorMessage from '../components/common/ErrorMessage'
import FoodCard from '../components/recommendation/FoodCard'
import Loading from '../components/common/Loading'
import { getRecommendations } from '../api/recommendations'
import { useAiLog } from '../contexts/AiLogContext'
import { parseApiError } from '../utils/apiError'
import '../styles/pages/ListPage.css'

/**
 * AI 추천 전체 목록 (STEP 13).
 * 대시보드는 앞부분만 보여주고, 여기서 전부 본다.
 *
 * `?category=한식` 이 붙어 있으면 그 분류만 요청한다.
 * 룰렛(Roulette)의 결과와 대시보드 카테고리 칩의 "더보기"가 이 쿼리로 들어온다.
 *
 * 쿼리에 key(KR)가 아니라 라벨(한식)을 담는 이유:
 * 서버가 분류를 이름으로 받으므로 변환 계층이 필요 없고,
 * 공유된 링크도 사람이 읽을 수 있다.
 *
 * 걸러내지 않고 다시 요청하는 이유
 * --------------------------------
 * 전체 목록을 받아 화면에서 거르면, '중식'을 골랐을 때 12개 중 두 개만 남아
 * 목록이 텅 빈다. 분류를 조건으로 넘겨서 그 분류로만 만들게 해야 한다.
 */

/** 한 번에 받아올 개수. 전체보기 화면이라 대시보드(5개)보다 넉넉히 잡는다 */
const PAGE_SIZE = 12

export default function RecommendPage() {
  const [params, setParams] = useSearchParams()
  // 주소에 category 가 아예 없으면 get() 이 null 을 준다. 그래서 ?. 로 막는다.
  // 여기는 ?? 가 아니라 || 를 쓴다. ?category= 처럼 빈 값이 붙어 온 경우
  // trim() 결과가 빈 문자열인데, 그것도 "분류 선택 안 함"으로 봐야 하기 때문이다.
  const category = params.get('category')?.trim() || ''

  const [items, setItems] = useState([])
  const [condition, setCondition] = useState('')
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  const { add } = useAiLog()

  useEffect(() => {
    let alive = true
    setLoading(true)
    setLoadError('')

    getRecommendations({
      source: '카테고리',
      limit: PAGE_SIZE,
      category: category || null,
    })
      .then((data) => {
        if (!alive) return

        setItems(data.items)
        setCondition(data.condition)

        const names = []
        for (const item of data.items) {
          names.push(item.food_name)
        }
        add({ source: '카테고리', condition: data.condition, foods: names })
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
  }, [category])

  /**
   * 카테고리 필터를 해제한다.
   *
   * replace: true 를 주면 뒤로가기 기록이 쌓이지 않는다.
   * 룰렛에서 들어온 사람이 뒤로가기를 누르면 룰렛 화면으로 바로 돌아가야 한다.
   */
  function clearCategory() {
    setParams({}, { replace: true })
  }

  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">오늘 뭐 먹지?</h2>
        {condition && <span className="muted section-more">{condition}</span>}
      </div>

      <p className="muted list-page__desc">
        등록하신 못 먹는 재료를 반영해 AI가 고른 메뉴입니다.
      </p>

      {/* 왜 걸러졌는지 보이지 않으면 목록이 짧아진 이유를 알 수 없다 */}
      {category && (
        <div className="list-page__filter-bar">
          <span className="list-page__chip">
            {category}
            <button
              type="button"
              className="list-page__chip-clear"
              onClick={clearCategory}
              aria-label={`${category} 필터 해제`}
            >
              ×
            </button>
          </span>
          <span className="muted">{items.length}건</span>
        </div>
      )}

      {loadError ? (
        <ErrorMessage message={loadError} />
      ) : loading ? (
        <Loading message="AI가 메뉴를 고르는 중..." />
      ) : items.length === 0 ? (
        <p className="empty">
          {category ? `${category} 추천을 찾지 못했습니다.` : '추천을 찾지 못했습니다.'}
          <br />
          <button type="button" className="btn btn--ghost btn--sm" onClick={clearCategory}>
            전체 보기
          </button>
        </p>
      ) : (
        <div className="list-page__cards">
          {items.map((food) => (
            <FoodCard key={food.food_name} food={food} />
          ))}
        </div>
      )}
    </div>
  )
}
