import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Icon from '../common/Icon'
import { useCatalog } from '../../contexts/CatalogContext'
import '../../styles/components/recommendation/FoodFinder.css'

// 첫 화면의 검색 카드.
//
// 상단바에도 검색창이 있지만 하는 일이 다르다.
// 상단바는 찾을 게 이미 정해진 사람이 쓰는 입력칸이고,
// 이 카드는 "뭘 검색할 수 있는지" 를 보여주는 예시 목록이 본체다.

// 접었을 때 보여줄 예시 개수. 나머지는 ⌄ 를 눌러야 나온다.
const FOLDED_COUNT = 5

export default function FoodFinder() {
  const [keyword, setKeyword] = useState('')
  const [expanded, setExpanded] = useState(false)

  const navigate = useNavigate()
  const { searchSuggestions } = useCatalog()

  let shown = searchSuggestions.slice(0, FOLDED_COUNT)
  if (expanded) {
    shown = searchSuggestions
  }

  // 검색 화면으로 이동한다.
  // encodeURIComponent 는 한글이나 공백을 주소에 넣을 수 있게 바꿔준다.
  function search(text) {
    const trimmed = text.trim()
    if (!trimmed) {
      // 빈 검색어로는 이동 안 한다
      return
    }

    navigate('/search?q=' + encodeURIComponent(trimmed))
  }

  function onSubmit(e) {
    e.preventDefault()
    search(keyword)
  }

  let moreLabel = '예시 더 보기'
  let caretClass = ''
  if (expanded) {
    moreLabel = '예시 접기'
    caretClass = 'is-up'
  }

  return (
    <section className="card finder">
      <form className="finder__search" onSubmit={onSubmit}>
        <input
          type="search"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="원하는 메뉴를 검색해보세요"
          maxLength={150}
          aria-label="메뉴 검색"
        />
        <button type="submit" className="btn finder__submit">
          <Icon name="search" size={18} />
          <span className="finder__submit-label">검색</span>
        </button>
      </form>

      <div className="finder__chips">
        <span className="finder__chips-label">인기 검색어</span>

        {shown.map(function (text) {
          return (
            <button
              key={text}
              type="button"
              className="chip"
              onClick={() => search(text)}
            >
              {text}
            </button>
          )
        })}

        {/* 예시를 처음부터 다 깔면 카드가 두세 줄로 늘어나서
            아래에 있는 예산이랑 카테고리가 화면 밖으로 밀린다 */}
        {searchSuggestions.length > FOLDED_COUNT && (
          <button
            type="button"
            className="finder__more"
            onClick={() => setExpanded(!expanded)}
            aria-label={moreLabel}
          >
            <span className={caretClass} aria-hidden="true">
              ⌄
            </span>
          </button>
        )}
      </div>
    </section>
  )
}
