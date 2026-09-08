import { useState } from 'react'
import { Link } from 'react-router-dom'
import ErrorMessage from '../components/common/ErrorMessage'
import Icon from '../components/common/Icon'
import FoodThumb from '../components/common/FoodThumb'
import Loading from '../components/common/Loading'
import { getRecommendations } from '../api/recommendations'
import { useAiLog } from '../contexts/AiLogContext'
import { useCatalog } from '../contexts/CatalogContext'
import { parseApiError } from '../utils/apiError'
import { formatMinutes, formatPrice } from '../utils/format'
import '../styles/pages/ListPage.css'
import '../styles/pages/FridgePage.css'

/**
 * 냉장고 재료로 찾기.
 *
 * "장을 안 보고 지금 있는 걸로 해결"이 목적이라, 정렬 기준이 다른 화면과 다르다.
 * 맛이나 인기가 아니라 **덜 사도 되는 순서**로 줄을 세운다.
 * 그래서 부족한 재료를 숨기지 않고 몇 개를 더 사야 하는지 같이 보여준다.
 *
 * 재료를 직접 적게만 두지 않고 칩을 먼저 깐 이유는, 빈 입력칸만 있으면
 * 뭘 어디까지 적어야 하는지 모르기 때문이다. 칩이 곧 예시다.
 *
 * 메뉴는 서버가 고른다. 고른 재료를 프롬프트에 넣어 그 재료로 만들 수 있는
 * 것만 생성하게 한다. 다 만들어놓고 화면에서 거르면 0건이 되기 쉽다.
 *
 * "몇 개 있어요 / 더 필요해요"는 화면에서 계산한다.
 * 서버가 메뉴마다 주재료 목록을 함께 내려주므로, 내가 고른 것과 맞춰보면 된다.
 * 이건 요청을 한 번 더 보낼 일이 아니다.
 *
 * 자동으로 조회하지 않고 버튼을 누르게 한 이유
 * --------------------------------------------
 * 칩을 하나 누를 때마다 AI 를 부르면, 재료 다섯 개를 고르는 동안 요청이
 * 다섯 번 나가고 그중 넷은 버려진다. 다 고르고 나서 한 번 부른다.
 */

/**
 * 내가 가진 재료 목록에 이 재료가 있는지 확인한다.
 *
 * 완전히 똑같은지만 보지 않는다.
 * '돼지고기'를 골랐는데 레시피에는 '돼지고기 앞다리살'이라고 적혀 있으면
 * 없는 것으로 처리돼서 결과가 이상해진다.
 * 그래서 한쪽이 다른 쪽을 품고 있으면 있는 것으로 본다.
 */
function hasIngredient(pickedNames, needItem) {
  for (const mine of pickedNames) {
    if (needItem.includes(mine) || mine.includes(needItem)) {
      return true
    }
  }
  return false
}

/**
 * 서버가 준 메뉴 목록에 "가진 것 / 더 살 것"을 붙이고 정렬한다.
 *
 * 각 항목은 이렇게 생겼다.
 *   { food: 음식, need: 필요한 재료 전체, have: 가진 것, missing: 더 사야 할 것 }
 */
function describeMatches(foods, pickedNames) {
  const matches = []

  for (const food of foods) {
    const need = food.ingredients ?? []

    // 필요한 재료를 "가진 것"과 "없는 것"으로 나눈다
    const have = []
    const missing = []

    for (const item of need) {
      if (hasIngredient(pickedNames, item)) {
        have.push(item)
      } else {
        missing.push(item)
      }
    }

    matches.push({ food, need, have, missing })
  }

  // 정렬 기준이 세 개다. 앞의 것이 같을 때만 다음 것을 본다.
  //   1) 더 사야 할 게 적은 순   <- 이 화면의 목적
  //   2) 가진 재료가 많은 순
  //   3) 싼 순
  matches.sort((a, b) => {
    if (a.missing.length !== b.missing.length) {
      return a.missing.length - b.missing.length
    }
    if (a.have.length !== b.have.length) {
      return b.have.length - a.have.length
    }
    return a.food.estimated_cost - b.food.estimated_cost
  })

  return matches
}

export default function FridgePage() {
  // 내가 고른 재료 이름들
  const [picked, setPicked] = useState([])
  // 직접 입력칸에 쓰는 중인 값
  const [draft, setDraft] = useState('')

  const [matches, setMatches] = useState([])
  const [searched, setSearched] = useState(false)
  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState('')

  const { fridgeGroups } = useCatalog()
  const { add } = useAiLog()

  /** 칩을 누르면 골랐다 <-> 안 골랐다를 뒤집는다. */
  function toggle(name) {
    setPicked((prev) => {
      if (prev.includes(name)) {
        return prev.filter((item) => item !== name)
      }
      return [...prev, name]
    })
  }

  /** 목록에 없는 재료를 직접 입력해서 추가한다. */
  function addCustom(e) {
    e.preventDefault() // 폼 기본 동작(새로고침)을 막는다

    const name = draft.trim()
    if (!name) return

    // 같은 재료를 두 번 넣으면 "가진 재료 개수"가 부풀려진다
    if (!picked.includes(name)) {
      setPicked([...picked, name])
    }

    setDraft('') // 입력칸을 비운다
  }

  /** '이 재료로 찾기' 버튼. 고른 재료를 서버에 넘긴다. */
  function search() {
    if (picked.length === 0) return

    setLoading(true)
    setLoadError('')
    setSearched(true)

    getRecommendations({
      source: '냉장고 재료',
      limit: 12,
      ingredients: picked,
    })
      .then((data) => {
        setMatches(describeMatches(data.items, picked))

        const names = []
        for (const item of data.items) {
          names.push(item.food_name)
        }
        add({ source: '냉장고 재료', condition: data.condition, foods: names })
      })
      .catch((err) => {
        setLoadError(parseApiError(err).message)
        setMatches([])
      })
      .finally(() => setLoading(false))
  }

  /** 전체 지우기. 결과도 같이 비운다. 안 그러면 재료는 없는데 목록만 남는다. */
  function clearAll() {
    setPicked([])
    setMatches([])
    setSearched(false)
    setLoadError('')
  }

  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">냉장고 재료로 찾기</h2>
        {picked.length > 0 && (
          <button type="button" className="section-more fridge__clear" onClick={clearAll}>
            전체 지우기
          </button>
        )}
      </div>

      <p className="muted list-page__desc">
        지금 있는 재료를 고르면, 덜 사도 되는 메뉴부터 보여드려요.
      </p>

      <div className="card fridge__picker">
        {fridgeGroups.map((group) => (
          <div key={group.key} className="fridge__group">
            <h3 className="fridge__group-title">{group.label}</h3>
            <div className="chip-row">
              {group.items.map((item) => (
                <button
                  key={item}
                  type="button"
                  className={`chip${picked.includes(item) ? ' is-active' : ''}`}
                  onClick={() => toggle(item)}
                  aria-pressed={picked.includes(item)}
                >
                  {item}
                </button>
              ))}
            </div>
          </div>
        ))}

        <form className="fridge__add" onSubmit={addCustom}>
          <input
            type="text"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            placeholder="목록에 없는 재료를 직접 넣어보세요"
            maxLength={30}
            aria-label="재료 직접 입력"
          />
          <button type="submit" className="btn btn--outline fridge__add-btn">
            <Icon name="plus" size={16} />
            추가
          </button>
        </form>

        {picked.length > 0 && (
          <p className="fridge__picked">
            고른 재료 <strong>{picked.length}개</strong> · {picked.join(' · ')}
          </p>
        )}

        <button
          type="button"
          className="btn btn--block fridge__search"
          onClick={search}
          disabled={picked.length === 0 || loading}
        >
          {loading ? 'AI가 찾아보는 중…' : '이 재료로 메뉴 찾기'}
        </button>
      </div>

      {loadError ? (
        <ErrorMessage message={loadError} />
      ) : loading ? (
        <Loading message="AI가 만들 수 있는 메뉴를 고르는 중..." />
      ) : picked.length === 0 ? (
        <p className="empty">재료를 하나 이상 골라주세요.</p>
      ) : !searched ? (
        <p className="empty">
          재료를 다 고르셨으면 위의 <strong>메뉴 찾기</strong> 버튼을 눌러주세요.
        </p>
      ) : matches.length === 0 ? (
        <p className="empty">
          고른 재료로 만들 수 있는 메뉴를 찾지 못했습니다.
          <br />
          재료를 몇 개 더 골라보세요.
        </p>
      ) : (
        <ul className="fridge__results">
          {matches.map(({ food, need, have, missing }) => (
            <li key={food.food_name} className="card fridge__result">
              <FoodThumb item={food} size="md" />

              <div className="fridge__result-body">
                <div className="fridge__result-head">
                  <h3 className="fridge__result-name">{food.food_name}</h3>
                  <span className="price">{formatPrice(food.estimated_cost)}</span>
                </div>

                <p className="fridge__match">
                  재료 {need.length}개 중{' '}
                  <strong>{have.length}개</strong> 있어요
                  <span className="muted">
                    {' · '}
                    {formatMinutes(food.cook_time_minutes)} · 난이도 {food.difficulty}
                  </span>
                </p>

                {missing.length > 0 ? (
                  <p className="fridge__missing">
                    <span className="muted">더 필요해요</span>{' '}
                    {missing.map((item) => (
                      <span key={item} className="tag">
                        {item}
                      </span>
                    ))}
                  </p>
                ) : (
                  <p className="fridge__ready">지금 바로 만들 수 있어요!</p>
                )}
              </div>

              <Link
                to={`/recipe/${encodeURIComponent(food.food_name)}`}
                className="btn btn--outline btn--sm fridge__result-cta"
              >
                레시피 보기
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
