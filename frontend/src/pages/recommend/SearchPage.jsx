import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import ErrorMessage from '../../components/common/ErrorMessage'
import Icon from '../../components/common/Icon'
import FoodCard from '../../components/common/FoodCard'
import Loading from '../../components/common/Loading'
import { getRecommendations } from '../../api/recommendations'
import { getCatalog } from '../../api/catalog'
import { addLog, foodNames } from '../../utils/aiLog'

// 레시피 검색 (/search?q=김치찌개).
// "8천원 이하 파스타" 같은 문장도 서버(AI)가 알아서 해석해준다.
export default function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const query = searchParams.get('q') || '' // 주소의 검색어

  const [keyword, setKeyword] = useState(query) // 입력칸에 치고 있는 글자
  const [items, setItems] = useState([])
  const [condition, setCondition] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [suggestions, setSuggestions] = useState([]) // 예시 검색어

  // 예시 검색어를 받아온다
  useEffect(() => {
    const load = async () => {
      try {
        const data = await getCatalog()
        setSuggestions(data.searchSuggestions || [])
      } catch (err) {
        // 예시가 안 보일 뿐이다
      }
    }
    load()
  }, [])

  // 주소의 검색어가 바뀌면 (상단바에서 검색한 경우 등) 입력칸도 맞추고 다시 검색한다
  useEffect(() => {
    setKeyword(query)

    if (!query) {
      setItems([])
      setCondition('')
      return
    }

    const load = async () => {
      setLoading(true)
      setError('')
      try {
        const data = await getRecommendations({ source: '레시피 검색', limit: 12, query: query })
        setItems(data.items || [])
        setCondition(data.condition)
        addLog('레시피 검색', data.condition, foodNames(data.items))
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
  }, [query])

  // 검색 버튼 → 주소의 ?q= 를 바꾼다 → 위 useEffect 가 검색한다
  const handleSubmit = (e) => {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다
    setSearchParams({ q: keyword.trim() })
  }

  return (
    // --wide : 카드 격자라 폭을 묶지 않고 본문을 꽉 채운다
    <div className="list-page list-page--wide">
      <div className="section-head">
        <h2 className="section-title">레시피 검색</h2>
        {query && !loading && <span className="muted section-more">{items.length}건</span>}
      </div>

      <form className="card search-form" onSubmit={handleSubmit}>
        <input
          type="search"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="메뉴 · 재료 · 예산 무엇으로든 찾아보세요"
          maxLength={150}
        />
        <button type="submit" className="btn search-form__submit">
          <Icon name="search" size={18} />
          검색
        </button>
      </form>

      {/* 서버가 검색어를 어떤 조건으로 이해했는지 */}
      {condition && !loading && <p className="muted search-page__read">{condition} 조건으로 찾았어요.</p>}

      {/* 검색어가 없으면 예시를 보여준다 */}
      {!query && (
        <div className="card search-page__hints">
          <h3 className="search-page__hints-title">이렇게 찾아보세요</h3>
          <div className="chip-row">
            {suggestions.map((text) => (
              <button key={text} type="button" className="chip" onClick={() => setSearchParams({ q: text })}>
                {text}
              </button>
            ))}
          </div>
          <p className="muted search-page__hints-foot">메뉴 이름뿐 아니라 재료 · 예산 · 상황으로도 찾을 수 있어요.</p>
        </div>
      )}

      {query && loading && <Loading message="AI가 찾아보는 중..." />}
      {query && error && <ErrorMessage message={error} />}

      {query && !loading && !error && items.length === 0 && (
        <p className="empty">
          '{query}'에 맞는 레시피를 찾지 못했습니다.
          <br />
          재료 이름이나 더 짧은 말로 다시 찾아보세요.
        </p>
      )}

      {query && !loading && !error && items.length > 0 && (
        <div className="list-page__cards">
          {items.map((food) => (
            <FoodCard key={food.foodName} food={food} />
          ))}
        </div>
      )}
    </div>
  )
}
