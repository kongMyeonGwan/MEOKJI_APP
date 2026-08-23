import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import Icon from '../components/common/Icon'
import FoodThumb from '../components/common/FoodThumb'
import {
  MOCK_FOOD_INGREDIENTS,
  MOCK_FRIDGE_GROUPS,
  MOCK_RECOMMENDATIONS,
} from '../mocks'
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
 * 매칭은 지금 화면에서 한다(mocks/MOCK_FOOD_INGREDIENTS).
 * 서버가 붙으면 고른 재료를 프롬프트에 넣어 AI가 만들 수 있는 것만 내려주게 된다.
 */

export default function FridgePage() {
  const [picked, setPicked] = useState([])
  const [draft, setDraft] = useState('')

  const toggle = (name) =>
    setPicked((prev) =>
      prev.includes(name) ? prev.filter((n) => n !== name) : [...prev, name]
    )

  const addCustom = (e) => {
    e.preventDefault()
    const name = draft.trim()
    if (!name) return
    // 같은 재료를 두 번 넣으면 매칭 개수가 부풀려진다
    if (!picked.includes(name)) setPicked((prev) => [...prev, name])
    setDraft('')
  }

  const matches = useMemo(() => {
    if (picked.length === 0) return []

    return MOCK_RECOMMENDATIONS.map((food) => {
      const need = MOCK_FOOD_INGREDIENTS[food.food_name] ?? []
      // '돼지고기'와 '돼지고기 앞다리살'처럼 한쪽이 다른 쪽을 품는 경우가 있어
      // 완전 일치만 보지 않는다
      const have = need.filter((item) =>
        picked.some((mine) => item.includes(mine) || mine.includes(item))
      )
      return { food, need, have, missing: need.filter((n) => !have.includes(n)) }
    })
      .filter((row) => row.have.length > 0)
      .sort(
        (a, b) =>
          // 더 사야 할 게 적은 순 -> 그다음 가진 재료가 많은 순
          a.missing.length - b.missing.length ||
          b.have.length - a.have.length ||
          a.food.estimated_cost - b.food.estimated_cost
      )
  }, [picked])

  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">냉장고 재료로 찾기</h2>
        {picked.length > 0 && (
          <button
            type="button"
            className="section-more fridge__clear"
            onClick={() => setPicked([])}
          >
            전체 지우기
          </button>
        )}
      </div>

      <p className="muted list-page__desc">
        지금 있는 재료를 고르면, 덜 사도 되는 메뉴부터 보여드려요.
      </p>

      <div className="card fridge__picker">
        {MOCK_FRIDGE_GROUPS.map((group) => (
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
      </div>

      {picked.length === 0 ? (
        <p className="empty">재료를 하나 이상 골라주세요.</p>
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
