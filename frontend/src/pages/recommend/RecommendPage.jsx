import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import ErrorMessage from '../../components/common/ErrorMessage'
import FoodCard from '../../components/common/FoodCard'
import Loading from '../../components/common/Loading'
import { getRecommendations } from '../../api/recommendations'
import { addLog, foodNames } from '../../utils/aiLog'

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
        setItems(data.items || [])
        setCondition(data.condition)
        addLog('카테고리', data.condition, foodNames(data.items))
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
    load()
  }, [category])

  // 카테고리 필터 풀기 (주소에서 ?category= 를 지운다)
  const clearCategory = () => {
    setSearchParams({})
  }

  return (
    // --wide : 카드 격자라 폭을 묶지 않고 본문을 꽉 채운다
    <div className="list-page list-page--wide">
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
            <FoodCard key={food.foodName} food={food} />
          ))}
        </div>
      )}
    </div>
  )
}
