import { formatMinutes } from '../../utils/format'
import '../../styles/components/recipe/RecipeView.css'

// 레시피 상세 (재료, 만드는 법, 대체 재료).
// RecipePage(추천)와 HistoryPage(저장된 기록) 에서 같이 쓴다.
//
// prices 에서 surveyed 가 false 인 재료는 정부 시세 조사 대상이 아닌 것(두부, 간장 등)이라
// 재료 옆에 "조사 대상 아님" 을 붙여준다.
export default function RecipeView({ recipe, ingredients, substitutes, prices }) {
  if (!recipe) {
    return null
  }

  // 값이 없을 수도 있어서 빈 배열로 바꿔둔다
  const ingredientList = ingredients || []
  const substituteList = substitutes || []
  const priceList = prices || []
  const steps = recipe.steps || []

  // 시세 조사 대상이 아닌 재료 이름. "두부 1모" 처럼 오면 앞 단어만 쓴다.
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
          {steps.map((step, index) => (
            <li key={index}>{step}</li>
          ))}
        </ol>
      </section>

      {substituteList.length > 0 && (
        <section>
          <h4 className="recipe__subtitle">대체 재료</h4>
          <ul className="recipe__substitutes">
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
