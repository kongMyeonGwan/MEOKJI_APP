import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import ErrorMessage from '../components/common/ErrorMessage'
import FoodCard from '../components/recommendation/FoodCard'
import Loading from '../components/common/Loading'
import { getRecommendations } from '../api/recommendations'
import { useAiLog } from '../contexts/AiLogContext'
import { parseApiError } from '../utils/apiError'
import '../styles/pages/ListPage.css'

// AI 추천 전체 목록.
// 첫 화면은 앞부분만 보여주고 여기서 전부 본다.
//
// 주소에 ?category=한식 이 붙어 있으면 그 분류만 요청한다.
// 룰렛 결과랑 첫 화면 카테고리 칩의 "더보기" 가 이 주소로 들어온다.
//
// 전체를 받아서 화면에서 거르지 않고 분류를 조건으로 넘겨서 다시 요청한다.
// 화면에서 거르면 '중식' 을 골랐을 때 12개 중 두 개만 남아서 목록이 텅 빈다.

// 한 번에 받아올 개수. 전체보기 화면이라 첫 화면(5개)보다 넉넉히 잡았다.
const PAGE_SIZE = 12

export default function RecommendPage() {
  const [params, setParams] = useSearchParams()

  // 주소에 category 가 없으면 get() 이 null 을 준다.
  // ?category= 처럼 빈 값이 붙어 올 수도 있어서 trim 까지 한다.
  let category = params.get('category')
  if (!category) {
    category = ''
  }
  category = category.trim()

  const [items, setItems] = useState([])
  const [condition, setCondition] = useState('')
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  const { add } = useAiLog()

  useEffect(() => {
    let alive = true
    setLoading(true)
    setLoadError('')

    let categoryParam = null
    if (category) {
      categoryParam = category
    }

    getRecommendations({
      source: '카테고리',
      limit: PAGE_SIZE,
      category: categoryParam,
    })
      .then(function (data) {
        if (!alive) {
          return
        }

        setItems(data.items)
        setCondition(data.condition)

        const names = []
        for (let i = 0; i < data.items.length; i++) {
          names.push(data.items[i].food_name)
        }

        add({ source: '카테고리', condition: data.condition, foods: names })
      })
      .catch(function (err) {
        if (alive) {
          setLoadError(parseApiError(err).message)
        }
      })
      .finally(function () {
        if (alive) {
          setLoading(false)
        }
      })

    return function () {
      alive = false
    }
  }, [category])

  // 카테고리 필터를 푼다.
  // replace: true 를 주면 뒤로가기 기록이 안 쌓인다.
  // 룰렛에서 들어온 사람이 뒤로가기를 누르면 룰렛으로 바로 가야 한다.
  function clearCategory() {
    setParams({}, { replace: true })
  }

  let emptyText = '추천을 찾지 못했습니다.'
  if (category) {
    emptyText = category + ' 추천을 찾지 못했습니다.'
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

      {/* 왜 걸러졌는지 안 보이면 목록이 짧아진 이유를 알 수 없다 */}
      {category && (
        <div className="list-page__filter-bar">
          <span className="list-page__chip">
            {category}
            <button
              type="button"
              className="list-page__chip-clear"
              onClick={clearCategory}
              aria-label={category + ' 필터 해제'}
            >
              ×
            </button>
          </span>
          <span className="muted">{items.length}건</span>
        </div>
      )}

      {loadError && <ErrorMessage message={loadError} />}

      {!loadError && loading && <Loading message="AI가 메뉴를 고르는 중..." />}

      {!loadError && !loading && items.length === 0 && (
        <p className="empty">
          {emptyText}
          <br />
          <button
            type="button"
            className="btn btn--ghost btn--sm"
            onClick={clearCategory}
          >
            전체 보기
          </button>
        </p>
      )}

      {!loadError && !loading && items.length > 0 && (
        <div className="list-page__cards">
          {items.map(function (food) {
            return <FoodCard key={food.food_name} food={food} />
          })}
        </div>
      )}
    </div>
  )
}
