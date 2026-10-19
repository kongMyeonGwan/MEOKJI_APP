import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import WeatherPick from './components/WeatherPick'
import BargainPick from './components/BargainPick'
import SubstituteFinder from './components/SubstituteFinder'
import GamePromo from '../../components/common/GamePromo'
import FoodFinder from './components/FoodFinder'
import BudgetPicker from './components/BudgetPicker'
import FoodCard from '../../components/common/FoodCard'
import PopularRecipes from './components/PopularRecipes'
import CategoryBar from './components/CategoryBar'
import ErrorMessage from '../../components/common/ErrorMessage'
import Loading from '../../components/common/Loading'
import { getCatalog } from '../../api/catalog'
import { getRecommendations } from '../../api/recommendations'
import { addLog, foodNames } from '../../utils/aiLog'

// 첫 화면 (대시보드).
//   왼쪽 : 날씨 추천 → 싼 재료 추천 → 메뉴 게임 → 검색 → 예산 → 카테고리 → 오늘의 추천
//   오른쪽 : 인기 레시피
export default function HomePage() {
  // 서버에서 받아오는 고정 목록들. 아래 카드들에 props 로 나눠준다.
  const [categories, setCategories] = useState([])
  const [games, setGames] = useState([])
  const [searchSuggestions, setSearchSuggestions] = useState([])
  const [budgetPresets, setBudgetPresets] = useState([])

  // 오늘의 추천
  const [category, setCategory] = useState('ALL') // 고른 카테고리 key. 'ALL' 이면 전체
  const [foods, setFoods] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // 처음 한 번, 고정 목록을 받아온다
  useEffect(() => {
    const load = async () => {
      try {
        const data = await getCatalog()
        setCategories(data.categories || [])
        setGames(data.games || [])
        setSearchSuggestions(data.search_suggestions || [])
        setBudgetPresets(data.budget_presets || [])
      } catch (err) {
        // 서버가 꺼져 있어도 화면은 떠야 한다. 빈 목록 그대로 둔다.
      }
    }
    load()
  }, [])

  // 고른 카테고리의 이름 ('KR' → '한식'). 전체면 null
  const selected = categories.find((item) => item.key === category)
  const categoryName = category !== 'ALL' && selected ? selected.label : null

  // 추천 더보기 주소. 제목 옆 '더보기' 와 마지막 더보기 카드가 같이 쓴다
  const moreLink = categoryName ? `/recommend?category=${categoryName}` : '/recommend'

  // 카테고리가 바뀔 때마다 추천을 다시 받아온다
  useEffect(() => {
    const load = async () => {
      setLoading(true)
      setError('')
      try {
        const data = await getRecommendations({ source: '카테고리', limit: 5, category: categoryName })
        setFoods(data.items || [])
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
  }, [categoryName])

  return (
    <div className="dash">
      <div className="dash__left">
        <WeatherPick />
        {/* 날씨 추천을 보고도 못 정했을 때 바로 권한다. 흰 카드 사이에 둬야 살구색 판이 눈에 띈다 */}
        <GamePromo games={games} />
        <FoodFinder searchSuggestions={searchSuggestions} />
        <BudgetPicker budgetPresets={budgetPresets} />

        {/* 카테고리는 아래 추천 목록을 거르는 버튼이라 한 카드에 같이 둔다 */}
        <section className="card dash__recs">
          <div className="section-head">
            <h2 className="section-title">오늘의 추천 레시피</h2>
            <Link to={moreLink} className="section-more">
              더보기 ›
            </Link>
          </div>

          <CategoryBar categories={categories} value={category} onChange={setCategory} />

          {loading && <Loading message="AI가 메뉴를 고르는 중..." />}
          {error && <ErrorMessage message={error} />}
          {!loading && !error && foods.length === 0 && <p className="empty">이 카테고리에는 아직 추천이 없어요.</p>}

          {!loading && !error && foods.length > 0 && (
            // [가로로 넘기는 음식 카드들][더보기 칸]
            // 더보기 칸은 스크롤 줄 밖에 둔다. 안에 두면 폭이 모자랄 때 줄 끝에서 잘린다
            <div className="dash__recs-row">
              <div className="dash__cards">
                {foods.map((food) => (
                  <FoodCard key={food.food_name} food={food} />
                ))}
              </div>

              <Link to={moreLink} className="dash__more-card">
                <span className="dash__more-icon">→</span>
                <strong>추천 더 보기</strong>
                <span className="muted">다른 메뉴도 골라보세요</span>
              </Link>
            </div>
          )}
        </section>
      </div>

      <div className="dash__right">
        <PopularRecipes />
        <BargainPick />
        {/* 싼 재료 → 대체 재료로 이어지는 재료 코너. 바로 아래 팁(AI 참고용)과 짝이다 */}
        <SubstituteFinder />

        <aside className="dash__tip">
          <span className="dash__tip-icon">💡</span>
          <div>
            <h3 className="dash__tip-title">AI 추천은 참고용이에요</h3>
            <p className="dash__tip-body">알레르기나 특이 체질이 있으신 경우 반드시 재료를 확인하고 섭취해주세요.</p>
          </div>
        </aside>
      </div>
    </div>
  )
}
