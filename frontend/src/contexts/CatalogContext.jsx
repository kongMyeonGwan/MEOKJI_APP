import { createContext, useContext, useEffect, useState } from 'react'
import { getCatalog } from '../api/catalog'

// 화면이 쓰는 고정 목록(카테고리, 룰렛, 게임, 밸런스 질문, 예시 검색어,
// 예산 프리셋)을 서버에서 한 번만 받아와 앱 전체가 나눠 쓴다.
// 화면마다 따로 부르면 첫 화면 하나 여는 데 같은 요청이 다섯 번 나간다.
//
// 서버가 안 떠 있으면 목록이 빈 배열로 남는다.
// 카테고리를 못 받았다고 화면 전체가 안 보이면 안 되기 때문이다.
//
// 쓰는 법:
//   const { categories, games, ready } = useCatalog()

const CatalogContext = createContext(null)

// 서버 응답이 오기 전(또는 실패했을 때)의 값
const EMPTY = {
  categories: [],
  roulette: [],
  games: [],
  balance_questions: [],
  search_suggestions: [],
  budget_presets: [],
}

export function CatalogProvider({ children }) {
  const [catalog, setCatalog] = useState(EMPTY)

  // 아직 받아오는 중인지. 목록이 비었을 때 "없음" 이랑 "로딩 중" 을 구분한다.
  const [ready, setReady] = useState(false)

  useEffect(() => {
    let alive = true

    getCatalog()
      .then(function (data) {
        if (alive) {
          setCatalog(data)
        }
      })
      .catch(function () {
        // 서버가 안 떠 있어도 앱은 떠야 한다. 빈 목록 그대로 둔다.
      })
      .finally(function () {
        if (alive) {
          setReady(true)
        }
      })

    // 화면을 떠난 뒤에 응답이 와도 setState 를 안 하게 막는다
    return function () {
      alive = false
    }
  }, [])

  const value = {
    ready: ready,
    categories: catalog.categories || [],
    roulette: catalog.roulette || [],
    games: catalog.games || [],
    balanceQuestions: catalog.balance_questions || [],
    searchSuggestions: catalog.search_suggestions || [],
    budgetPresets: catalog.budget_presets || [],
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
