import { createContext, useContext, useEffect, useState } from 'react'
import { getCatalog } from '../api/catalog'

/**
 * 화면이 쓰는 고정 목록 (카테고리 · 룰렛 · 게임 · 밸런스 질문 · 냉장고 재료 ·
 * 예시 검색어 · 예산 프리셋).
 *
 * 서버에서 한 번만 받아와 앱 전체가 나눠 쓴다.
 * 화면마다 따로 부르면 대시보드 하나 여는 데 같은 요청이 다섯 번 나간다.
 *
 * 왜 서버에서 받아오나
 * --------------------
 * 예전에는 mocks/index.js 에 하드코딩되어 있었다. 그러면 냉장고 재료 칩의
 * 글자가 서버가 아는 재료 이름과 어긋나는 순간, 아무것도 매칭되지 않는데
 * 화면만 봐서는 이유를 알 수 없다. 한 곳(app/data/catalog.json)에서 관리한다.
 *
 * 서버가 안 떠 있을 때
 * --------------------
 * 목록이 빈 배열로 남는다. 화면은 "칩이 없는 상태"로 그려지고 에러 페이지가
 * 되지 않는다. 카테고리를 못 받았다고 대시보드 전체가 안 보이면 안 된다.
 *
 * 쓰는 법
 * -------
 *   const { categories, games, ready } = useCatalog()
 */

const CatalogContext = createContext(null)

/** 서버 응답이 오기 전(또는 실패했을 때)의 값. 전부 빈 목록이다. */
const EMPTY = {
  categories: [],
  roulette: [],
  games: [],
  balance_questions: [],
  fridge_groups: [],
  search_suggestions: [],
  budget_presets: [],
}

export function CatalogProvider({ children }) {
  const [catalog, setCatalog] = useState(EMPTY)

  // 아직 받아오는 중인지. 목록이 비었을 때 "없음"과 "로딩 중"을 구분한다.
  const [ready, setReady] = useState(false)

  useEffect(() => {
    let alive = true

    getCatalog()
      .then((data) => {
        if (alive) setCatalog(data)
      })
      .catch(() => {
        // 서버가 안 떠 있어도 앱은 떠야 한다. 빈 목록 그대로 둔다.
      })
      .finally(() => {
        if (alive) setReady(true)
      })

    // 화면을 떠난 뒤에 응답이 도착해도 setState 를 부르지 않게 한다
    return () => {
      alive = false
    }
  }, [])

  const value = {
    ready,
    categories: catalog.categories ?? [],
    roulette: catalog.roulette ?? [],
    games: catalog.games ?? [],
    balanceQuestions: catalog.balance_questions ?? [],
    fridgeGroups: catalog.fridge_groups ?? [],
    searchSuggestions: catalog.search_suggestions ?? [],
    budgetPresets: catalog.budget_presets ?? [],
  }

  return (
    <CatalogContext.Provider value={value}>{children}</CatalogContext.Provider>
  )
}

export function useCatalog() {
  const context = useContext(CatalogContext)

  if (context === null) {
    throw new Error('useCatalog()는 <CatalogProvider> 안에서만 쓸 수 있습니다.')
  }

  return context
}
