import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import ErrorMessage from '../../components/common/ErrorMessage'
import FoodCard from '../../components/recommendation/FoodCard'
import Loading from '../../components/common/Loading'
import { getRecommendations } from '../../api/recommendations'
import { addLog, foodNames } from '../../utils/aiLog'
import { getErrorMessage } from '../../utils/apiError'
import '../../styles/pages/ListPage.css'

// AI 추천 전체 목록 (/recommend).
// 주소에 ?category=한식 이 붙어 있으면 그 종류만 추천받는다.
export default function RecommendPage() {
  // useSearchParams: 주소의 ?뒤 값을 읽고 바꾸는 도구
  const [searchParams, setSearchParams] = useSearchParams()
  const category = searchParams.get('category') || ''

  const [items, setItems] = useState([])
  const [condition, setCondition] = useState('') // 서버가 알려주는 추천 조건 요약
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // 카테고리가 바뀔 때마다 다시 받아온다
  useEffect(() => {
    const load = async () => {
      setLoading(true)
      setError('')
      try {
        const data = await getRecommendations({ source: '카테고리', limit: 12, category: category })
        setItems(data.items)
        setCondition(data.condition)
        addLog('카테고리', data.condition, foodNames(data.items))
      } catch (err) {
        setError(getErrorMessage(err))
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [category])

  // 카테고리 필터 풀기 (주소에서 ?category= 를 지운다)
  const clearCategory = () => {
    setSearchParams({})
  }

  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">오늘 뭐 먹지?</h2>
        {condition && <span className="muted section-more">{condition}</span>}
      </div>

      <p className="muted list-page__desc">등록하신 못 먹는 재료를 반영해 AI가 고른 메뉴입니다.</p>

      {category && (
        <div className="list-page__filter-bar">
          <span className="list-page__chip">
            {category}
            <button type="button" className="list-page__chip-clear" onClick={clearCategory}>
              ×
            </button>
          </span>
          <span className="muted">{items.length}건</span>
        </div>
      )}

      {loading && <Loading message="AI가 메뉴를 고르는 중..." />}
      {error && <ErrorMessage message={error} />}

      {!loading && !error && items.length === 0 && (
        <p className="empty">
          {category} 추천을 찾지 못했습니다.
          <br />
          <button type="button" className="btn btn--ghost btn--sm" onClick={clearCategory}>
            전체 보기
          </button>
        </p>
      )}

      {!loading && !error && items.length > 0 && (
        <div className="list-page__cards">
          {items.map((food) => (
            <FoodCard key={food.food_name} food={food} />
          ))}
        </div>
      )}
    </div>
  )
}
