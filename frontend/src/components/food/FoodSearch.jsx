import { useState } from 'react'
import '../../styles/components/food/FoodSearch.css'

/**
 * 먹고 싶은 음식을 직접 입력해 레시피를 생성한다 (STEP 11).
 * 지금은 화면만 있고, 실제 AI 호출은 나중에 onSearch에 연결한다.
 */
export default function FoodSearch({ onSearch }) {
  const [keyword, setKeyword] = useState('')

  const onSubmit = (e) => {
    e.preventDefault()
    const trimmed = keyword.trim()
    if (!trimmed) return
    onSearch?.(trimmed)
  }

  return (
    <form className="food-search" onSubmit={onSubmit}>
      <input
        type="text"
        value={keyword}
        onChange={(e) => setKeyword(e.target.value)}
        placeholder="먹고 싶은 음식을 입력해보세요 (예: 김치찌개)"
        maxLength={150}
        aria-label="음식 검색"
      />
      <button type="submit" className="btn">
        레시피 찾기
      </button>
    </form>
  )
}
