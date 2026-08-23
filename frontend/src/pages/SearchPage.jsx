import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import Icon from '../components/common/Icon'
import FoodCard from '../components/recommendation/FoodCard'
import {
  MOCK_FOOD_INGREDIENTS,
  MOCK_RECOMMENDATIONS,
  MOCK_SEARCH_SUGGESTIONS,
} from '../mocks'
import { formatPrice } from '../utils/format'
import '../styles/pages/ListPage.css'
import '../styles/pages/SearchPage.css'

/**
 * 레시피 검색.
 *
 * 검색어를 쿼리스트링(`?q=`)에 둔다. 상단바·대시보드·AI 도우미가 전부
 * 이 화면으로 보내는데, state로만 들고 있으면 새로고침이나 링크 공유에서 값이 사라진다.
 *
 * **여기의 매칭 로직은 임시다.** 실제로는 검색어를 그대로 AI에 넘겨
 * "8천원 이하 파스타" 같은 문장을 서버가 해석하게 된다(STEP 13).
 * 화면에서 흉내만 내는 이유는, 문장으로 물어봐도 뭔가 나온다는 걸
 * 붙이기 전에 확인해야 검색 UI를 제대로 만들 수 있기 때문이다.
 */

/**
 * "8천원 이하", "1만원 이내" 같은 표현에서 상한을 뽑는다.
 * 못 찾으면 null이고, 그때는 금액 조건 없이 글자만 본다.
 */
function readBudget(text) {
  if (!/이하|이내|미만|아래/.test(text)) return null

  const man = text.match(/(\d+)\s*만\s*(\d+)?\s*천?/)
  if (man) return Number(man[1]) * 10000 + Number(man[2] ?? 0) * 1000

  const chun = text.match(/(\d+)\s*천/)
  if (chun) return Number(chun[1]) * 1000

  const won = text.match(/([\d,]{3,})\s*원/)
  if (won) return Number(won[1].replace(/,/g, ''))

  return null
}

/** 음식 한 건이 검색어와 얼마나 맞는지. 0이면 결과에서 뺀다 */
function score(food, words) {
  const ingredients = MOCK_FOOD_INGREDIENTS[food.food_name] ?? []

  return words.reduce((sum, word) => {
    // 이름이 맞으면 가장 강하게 올린다. "김치찌개"를 찾는 사람에게
    // 재료로 김치가 들어간 다른 메뉴가 먼저 나오면 검색이 고장 난 것처럼 보인다
    if (food.food_name.includes(word)) return sum + 5
    if (food.category.includes(word)) return sum + 3
    if (food.tags?.some((tag) => tag.includes(word))) return sum + 3
    if (ingredients.some((item) => item.includes(word) || word.includes(item))) {
      return sum + 2
    }
    if (food.reason.includes(word)) return sum + 1
    return sum
  }, 0)
}

export default function SearchPage() {
  const [params, setParams] = useSearchParams()
  const query = params.get('q')?.trim() ?? ''

  const [draft, setDraft] = useState(query)

  // 상단바에서 다른 검색어로 들어오면 입력칸도 따라가야 한다.
  // 이 화면에 머문 채 URL만 바뀌는 경우가 있어서 query를 지켜본다
  useEffect(() => setDraft(query), [query])

  const budget = useMemo(() => readBudget(query), [query])

  const { results, budgetOnly } = useMemo(() => {
    if (!query) return { results: [], budgetOnly: false }

    // 조사·기호를 떼는 형태소 분석은 서버 몫이다.
    // 여기서는 공백으로만 끊고, 한 글자짜리는 아무 데나 걸려서 뺀다
    const words = query
      .split(/[\s,]+/)
      .map((w) => w.replace(/[?!.]/g, ''))
      .filter((w) => w.length > 1)

    const affordable = MOCK_RECOMMENDATIONS.filter(
      (food) => budget === null || food.estimated_cost <= budget
    )
      .map((food) => ({ food, point: score(food, words) }))
      .sort((a, b) => b.point - a.point || a.food.estimated_cost - b.food.estimated_cost)

    const matched = affordable.filter((row) => row.point > 0)

    // 글자가 하나도 안 맞아도 금액 조건이 있으면 빈 화면을 주지 않는다.
    // "8천원 이하 파스타"에 해당하는 게 없을 때 아무것도 없다고 하는 것보다
    // 그 예산으로 되는 메뉴를 보여주는 편이 낫다. 대신 아래에서 그렇다고 알린다.
    if (matched.length === 0 && budget !== null) {
      return { results: affordable.map((row) => row.food), budgetOnly: true }
    }

    return { results: matched.map((row) => row.food), budgetOnly: false }
  }, [query, budget])

  const submit = (e) => {
    e.preventDefault()
    const trimmed = draft.trim()
    // 빈 검색어로 URL을 더럽히지 않는다. 지우고 누르면 안내 화면으로 돌아간다
    setParams(trimmed ? { q: trimmed } : {}, { replace: true })
  }

  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">레시피 검색</h2>
        {query && <span className="muted section-more">{results.length}건</span>}
      </div>

      <form className="card search-form" onSubmit={submit} role="search">
        <input
          type="search"
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          placeholder="메뉴 · 재료 · 예산 무엇으로든 찾아보세요"
          maxLength={150}
          aria-label="검색어"
          autoFocus
        />
        <button type="submit" className="btn search-form__submit">
          <Icon name="search" size={18} />
          검색
        </button>
      </form>

      {/* 검색어에서 읽어낸 조건을 되돌려준다.
          "8천원 이하"가 정말 금액으로 해석됐는지 알 수 있어야 한다 */}
      {budget !== null && (
        <p className="muted search-page__read">
          예산 조건을 <strong>{formatPrice(budget)} 이하</strong>로 읽었어요.
          {budgetOnly &&
            ' 조건에 딱 맞는 메뉴가 없어 이 예산으로 되는 메뉴를 보여드려요.'}
        </p>
      )}

      {!query ? (
        <div className="card search-page__hints">
          <h3 className="search-page__hints-title">이렇게 찾아보세요</h3>
          <div className="chip-row">
            {MOCK_SEARCH_SUGGESTIONS.map((text) => (
              <button
                key={text}
                type="button"
                className="chip"
                onClick={() => setParams({ q: text })}
              >
                {text}
              </button>
            ))}
          </div>
          <p className="muted search-page__hints-foot">
            메뉴 이름뿐 아니라 재료 · 예산 · 상황으로도 찾을 수 있어요.
          </p>
        </div>
      ) : results.length === 0 ? (
        <p className="empty">
          &lsquo;{query}&rsquo;에 맞는 레시피를 찾지 못했습니다.
          <br />
          재료 이름이나 더 짧은 말로 다시 찾아보세요.
        </p>
      ) : (
        <div className="list-page__cards">
          {results.map((food) => (
            <FoodCard key={food.food_name} food={food} />
          ))}
        </div>
      )}
    </div>
  )
}
