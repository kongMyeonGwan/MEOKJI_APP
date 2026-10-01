import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Icon from '../../../components/common/Icon'
import './FoodFinder.css'

// 첫 화면의 검색 카드. 검색창 + 예시 검색어 칩.
// 예시는 처음에 5개만 보이고 ⌄ 를 누르면 전부 펼쳐진다.
export default function FoodFinder({ searchSuggestions }) {
  const [keyword, setKeyword] = useState('')
  const [expanded, setExpanded] = useState(false)

  const navigate = useNavigate()

  const shown = expanded ? searchSuggestions : searchSuggestions.slice(0, 5)

  // 검색 화면으로 이동한다
  const search = (text) => {
    if (!text.trim()) {
      return
    }
    navigate(`/search?q=${encodeURIComponent(text.trim())}`)
  }

  const handleSubmit = (e) => {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다
    search(keyword)
  }

  return (
    <section className="card finder">
      <form className="finder__search" onSubmit={handleSubmit}>
        <input
          type="search"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="원하는 메뉴를 검색해보세요"
          maxLength={150}
        />
        <button type="submit" className="btn finder__submit">
          <Icon name="search" size={18} />
          <span className="finder__submit-label">검색</span>
        </button>
      </form>

      <div className="finder__chips">
        <span className="finder__chips-label">인기 검색어</span>

        {shown.map((text) => (
          <button key={text} type="button" className="chip" onClick={() => search(text)}>
            {text}
          </button>
        ))}

        {searchSuggestions.length > 5 && (
          <button type="button" className="finder__more" onClick={() => setExpanded(!expanded)}>
            <span className={expanded ? 'is-up' : ''}>⌄</span>
          </button>
        )}
      </div>
    </section>
  )
}
