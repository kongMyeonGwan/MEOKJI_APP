import { formatMinutes } from '../../utils/format'

// 레시피 상세 (재료, 만드는 법, 대체 재료).
// RecipePage(추천)와 HistoryPage(저장된 기록) 에서 같이 쓴다.
//
// prices 에서 surveyed 가 false 인 재료는 정부 시세 조사 대상이 아닌 것(두부, 간장 등)이라
// 재료 옆에 "조사 대상 아님" 을 붙여준다.
//
// 왜 그 표시가 필요한가 — 두부와 간장이 빠진 합계는 실제 장바구니보다
// 적다. 아무 말 없이 "재료비 8,400원" 으로 두면 사용자가 그 값을
// 믿고 장을 보러 간다.
//
// 네 가지를 따로 받는 이유 — 서버 응답의 칸 이름이 쓰는 화면마다
// 다르다. 레시피 화면은 recipe_json 으로 받고, 기록 화면은 저장된
// 값을 받는다. 객체 하나로 받으면 쓰는 쪽이 모양을 맞춰야 한다.
export default function RecipeView({ recipe, ingredients, substitutes, prices }) {
  // 레시피가 없으면 아무것도 안 그린다. 쓰는 쪽에서 조건부로
  // 감싸지 않아도 되게 하려는 것이다
  if (!recipe) {
    return null
  }

  // 값이 없을 수도 있어서 빈 배열로 바꿔둔다
  //
  // 대체 재료는 못 먹는 재료를 등록하지 않은 사람에게 null 로 오고,
  // 가격은 기록 화면에서 아예 안 넘어온다. || [] 가 없으면
  // .map 에서 터진다
  const ingredientList = ingredients || []
  const substituteList = substitutes || []
  const priceList = prices || []
  const steps = recipe.steps || []

  // 시세 조사 대상이 아닌 재료 이름. "두부 1모" 처럼 오면 앞 단어만 쓴다.
  //
  // 서버가 이름에 분량을 붙여 보내는데(PriceItemVO.name), 아래에서
  // 재료 목록의 이름과 견주려면 분량을 떼야 한다.
  //
  // === false 로 견주는 이유 — surveyed 가 아예 안 온 경우(기록 화면)를
  // 빼야 한다. !item.surveyed 로 적으면 undefined 도 걸려서,
  // 모든 재료에 "조사 대상 아님" 이 붙는다
  const unsurveyed = priceList
    .filter((item) => item.surveyed === false)
    .map((item) => item.name.split(' ')[0])

  return (
    <div className="recipe">
      <div className="recipe__meta">
        <span className="tag">⏱ {formatMinutes(recipe.cook_time_minutes)}</span>
        <span className="tag">{recipe.servings}인분</span>
        <span className="tag">난이도 {recipe.difficulty}</span>
      </div>

      <section>
        <h4 className="recipe__subtitle">재료</h4>
        <ul className="recipe__ingredients">
          {/* key 를 이름으로 쓴다. 한 레시피에 같은 재료가 두 번
              나오지 않으므로 겹치지 않는다 */}
          {ingredientList.map((item) => (
            <li key={item.name}>
              <span>
                {item.name}
                {unsurveyed.includes(item.name) && <span className="recipe__no-price">조사 대상 아님</span>}
              </span>
              <span className="muted">
                {item.quantity}
                {item.unit}
              </span>
            </li>
          ))}
        </ul>

        {/* 재료 옆의 작은 표시만으로는 "그래서 합계가 왜 적은지" 를
            알 수 없다. 아래에 한 번 더 풀어 적는다.

            하나라도 있을 때만 그린다 - 없으면 설명할 것이 없다 */}
        {unsurveyed.length > 0 && (
          <p className="recipe__note">
            <strong>특이사항</strong>
            <span className="recipe__note-names">{unsurveyed.join(' · ')}</span>
            농수산물 시세 조사 대상이 아닌 재료라 아래 재료비 합계에서 빠져 있어요.
            실제로 장을 보면 그만큼 더 나옵니다.
          </p>
        )}
      </section>

      <section>
        <h4 className="recipe__subtitle">만드는 법</h4>
        <ol className="recipe__steps">
          {/* 여기만 key 로 index 를 쓴다. 조리 순서는 글자라서
              겹칠 수 있고("중불로 줄인다" 가 두 번 나올 수 있다),
              순서가 바뀌거나 중간에 끼어들지도 않는다 */}
          {steps.map((step, index) => (
            <li key={index}>{step}</li>
          ))}
        </ol>
      </section>

      {/* 대체 재료. 바꿀 것이 있을 때만 이 묶음을 그린다.
          못 먹는 재료를 등록하지 않은 사람에게는 빈 제목만 남는다 */}
      {substituteList.length > 0 && (
        <section>
          <h4 className="recipe__subtitle">대체 재료</h4>
          <ul className="recipe__substitutes">
            {/* 왜 바꿨는지(reason)까지 같이 보여준다. 안 보여주면
                사용자는 레시피가 원래 그런 줄 안다 */}
            {substituteList.map((sub) => (
              <li key={sub.original}>
                <strong>{sub.original}</strong> → {sub.substitute}
                <span className="muted"> · {sub.reason}</span>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  )
}
