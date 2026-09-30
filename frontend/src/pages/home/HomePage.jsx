import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import WeatherPick from '../../components/recommendation/WeatherPick'
import BargainPick from '../../components/recommendation/BargainPick'
import GamePromo from '../../components/recommendation/GamePromo'
import FoodFinder from '../../components/recommendation/FoodFinder'
import BudgetPicker from '../../components/recommendation/BudgetPicker'
import FoodCard from '../../components/recommendation/FoodCard'
import PopularRecipes from '../../components/recommendation/PopularRecipes'
import CategoryBar from '../../components/common/CategoryBar'
import ErrorMessage from '../../components/common/ErrorMessage'
import Loading from '../../components/common/Loading'
import { getCatalog } from '../../api/catalog'
import { getRecommendations } from '../../api/recommendations'
import { addLog, foodNames } from '../../utils/aiLog'
import { getErrorMessage } from '../../utils/apiError'
import '../../styles/pages/HomePage.css'

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
        setCategories(data.categories)
        setGames(data.games)
        setSearchSuggestions(data.search_suggestions)
        setBudgetPresets(data.budget_presets)
      } catch (err) {
        // 서버가 꺼져 있어도 화면은 떠야 한다. 빈 목록 그대로 둔다.
      }
    }
    load()
  }, [])

  // 고른 카테고리의 이름 ('KR' → '한식'). 전체면 null
  const selected = categories.find((item) => item.key === category)
  const categoryName = category !== 'ALL' && selected ? selected.label : null

  // 카테고리가 바뀔 때마다 추천을 다시 받아온다
  useEffect(() => {
    const load = async () => {
      setLoading(true)
      setError('')
      try {
        const data = await getRecommendations({ source: '카테고리', limit: 5, category: categoryName })
        setFoods(data.items)
        addLog('카테고리', data.condition, foodNames(data.items))
      } catch (err) {
        setError(getErrorMessage(err))
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
        <BargainPick />
        <GamePromo games={games} />
        <FoodFinder searchSuggestions={searchSuggestions} />
        <BudgetPicker budgetPresets={budgetPresets} />

        <section>
          <h2 className="section-title dash__cats-title">카테고리</h2>
          <CategoryBar categories={categories} value={category} onChange={setCategory} />
        </section>

        <section>
          <div className="section-head">
            <h2 className="section-title">오늘의 추천 레시피</h2>
            <Link to={categoryName ? `/recommend?category=${categoryName}` : '/recommend'} className="section-more">
              더보기 ›
            </Link>
          </div>

          {loading && <Loading message="AI가 메뉴를 고르는 중..." />}
          {error && <ErrorMessage message={error} />}
          {!loading && !error && foods.length === 0 && <p className="empty">이 카테고리에는 아직 추천이 없어요.</p>}

          {!loading && !error && foods.length > 0 && (
            <div className="dash__cards">
              {foods.map((food) => (
                <FoodCard key={food.food_name} food={food} />
              ))}
            </div>
          )}
        </section>
      </div>

      <div className="dash__right">
        <PopularRecipes />

        <aside className="dash__tip">
          <span className="dash__tip-icon">💡</span>
          <div>
            <h3 className="dash__tip-title">AI 추천은 참고용이에요</h3>
            <p className="dash__tip-body">
              알레르기나 특이 체질이 있으신 경우 반드시 재료를 확인하고 섭취해주세요.
            </p>
          </div>
        </aside>
      </div>
    </div>
  )
}
