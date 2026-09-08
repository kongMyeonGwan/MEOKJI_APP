import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import ErrorMessage from '../components/common/ErrorMessage'
import Icon from '../components/common/Icon'
import FoodCard from '../components/recommendation/FoodCard'
import Loading from '../components/common/Loading'
import { getRecommendations } from '../api/recommendations'
import { useAiLog } from '../contexts/AiLogContext'
import { useCatalog } from '../contexts/CatalogContext'
import { parseApiError } from '../utils/apiError'
import '../styles/pages/ListPage.css'
import '../styles/pages/SearchPage.css'

/**
 * 레시피 검색.
 *
 * 검색어를 주소창(?q=김치찌개)에 둔다.
 * 상단바 · 대시보드 · AI 도우미가 전부 이 화면으로 보내는데,
 * useState 로만 들고 있으면 새로고침하거나 링크를 공유했을 때 검색어가 사라진다.
 *
 * 검색어를 손대지 않고 그대로 서버에 넘긴다
 * -----------------------------------------
 * "8천원 이하 파스타" 같은 문장을 해석하는 게 목적이라 SQL LIKE 로는 안 되고,
 * 화면에서 정규식으로 금액을 뽑아내는 것도 한계가 분명하다
 * ("만원 조금 안 되게", "월급날이라 좀 비싸도 돼" 는 못 읽는다).
 * 문장 해석은 AI 가 할 일이고, 이 화면은 입력칸과 결과 목록만 담당한다.
 */

/** 한 번에 받아올 개수 */
const PAGE_SIZE = 12

export default function SearchPage() {
  const [params, setParams] = useSearchParams()

  // 주소창의 ?q= 값. 없으면 빈 문자열
  const query = params.get('q')?.trim() ?? ''

  // 입력칸에 지금 타이핑 중인 값. 검색 버튼을 눌러야 query 가 된다
  const [draft, setDraft] = useState(query)

  const [items, setItems] = useState([])
  const [condition, setCondition] = useState('')
  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState('')

  const { searchSuggestions } = useCatalog()
  const { add } = useAiLog()

  // 상단바에서 다른 검색어로 들어오면 입력칸도 따라가야 한다.
  // 이 화면에 머문 채 주소만 바뀌는 경우가 있어서 query 를 지켜본다.
  useEffect(() => {
    setDraft(query)
  }, [query])

  // 검색어가 바뀌면 서버에 물어본다.
  // 검색어가 없으면(처음 들어온 상태) 요청하지 않고 안내 화면을 보여준다.
  useEffect(() => {
    if (!query) {
      setItems([])
      setCondition('')
      setLoadError('')
      return
    }

    let alive = true
    setLoading(true)
    setLoadError('')

    getRecommendations({
      source: '레시피 검색',
      limit: PAGE_SIZE,
      query,
    })
      .then((data) => {
        if (!alive) return

        setItems(data.items)
        setCondition(data.condition)

        const names = []
        for (const item of data.items) {
          names.push(item.food_name)
        }
        add({ source: '레시피 검색', condition: data.condition, foods: names })
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
  }, [query])

  function submit(e) {
    e.preventDefault() // 폼 기본 동작(새로고침)을 막는다
    const trimmed = draft.trim()

    // 빈 검색어로 주소를 더럽히지 않는다. 지우고 누르면 안내 화면으로 돌아간다.
    if (trimmed) {
      setParams({ q: trimmed }, { replace: true })
    } else {
      setParams({}, { replace: true })
    }
  }

  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">레시피 검색</h2>
        {query && !loading && (
          <span className="muted section-more">{items.length}건</span>
        )}
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

      {/* 서버가 검색어를 어떤 조건으로 읽었는지 되돌려준다.
          "8천원 이하"가 정말 금액으로 해석됐는지 사용자가 알 수 있어야 한다 */}
      {condition && !loading && (
        <p className="muted search-page__read">{condition} 조건으로 찾았어요.</p>
      )}

      {/* 화면은 네 가지 중 하나다: 검색 전 안내 / 로딩 / 결과 없음 / 결과 목록 */}
      {!query ? (
        <div className="card search-page__hints">
          <h3 className="search-page__hints-title">이렇게 찾아보세요</h3>
          <div className="chip-row">
            {searchSuggestions.map((text) => (
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
      ) : loadError ? (
        <ErrorMessage message={loadError} />
      ) : loading ? (
        <Loading message="AI가 찾아보는 중..." />
      ) : items.length === 0 ? (
        <p className="empty">
          &lsquo;{query}&rsquo;에 맞는 레시피를 찾지 못했습니다.
          <br />
          재료 이름이나 더 짧은 말로 다시 찾아보세요.
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
