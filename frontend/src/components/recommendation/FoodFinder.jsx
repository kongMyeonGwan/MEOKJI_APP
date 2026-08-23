import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Icon from '../common/Icon'
import { MOCK_SEARCH_SUGGESTIONS } from '../../mocks'
import '../../styles/components/recommendation/FoodFinder.css'

/**
 * 검색 카드 — 직접 찾기.
 *
 * 상단바에도 검색창이 있지만 하는 일이 다르다.
 * 상단바는 "찾을 게 이미 정해진 사람"이 쓰는 입력칸이고,
 * 이 카드는 **뭘 검색할 수 있는지 보여주는** 예시 목록이 본체다.
 * 재료로도, 예산으로도, 문장으로도 물어볼 수 있다는 걸 칩으로 알린다.
 *
 * 그래서 대시보드 맨 위에 두지 않는다. 1순위는 날씨에 맞는 음식을
 * **먼저 보여주는** 것이고(WeatherPick), 이 카드는 그게 안 당길 때 쓰는 도구다.
 */

/** 접었을 때 보여줄 예시 개수. 나머지는 ⌄ 를 눌러야 나온다 */
const FOLDED_COUNT = 5

export default function FoodFinder() {
  const [keyword, setKeyword] = useState('')
  const [expanded, setExpanded] = useState(false)
  const navigate = useNavigate()

  const shown = expanded
    ? MOCK_SEARCH_SUGGESTIONS
    : MOCK_SEARCH_SUGGESTIONS.slice(0, FOLDED_COUNT)

  const search = (text) => {
    const trimmed = text.trim()
    if (!trimmed) return
    navigate(`/search?q=${encodeURIComponent(trimmed)}`)
  }

  return (
    <section className="card finder">
      <form
        className="finder__search"
        onSubmit={(e) => {
          e.preventDefault()
          search(keyword)
        }}
        role="search"
      >
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

        {shown.map((text) => (
          <button
            key={text}
            type="button"
            className="chip"
            onClick={() => search(text)}
          >
            {text}
          </button>
        ))}

        {/* 예시를 처음부터 다 깔면 카드가 두 줄 세 줄로 늘어나
            아래에 있는 예산·카테고리가 화면 밖으로 밀린다 */}
        {MOCK_SEARCH_SUGGESTIONS.length > FOLDED_COUNT && (
          <button
            type="button"
            className="finder__more"
            onClick={() => setExpanded((prev) => !prev)}
            aria-expanded={expanded}
            aria-label={expanded ? '예시 접기' : '예시 더 보기'}
          >
            <span className={expanded ? 'is-up' : ''} aria-hidden="true">
              ⌄
            </span>
          </button>
        )}
      </div>
    </section>
  )
}
