import { formatMinutes } from '../../utils/format'
import '../../styles/components/recipe/RecipeView.css'

/**
 * 레시피 상세 표시.
 * 추천 결과(아직 저장 전)와 History(저장된 스냅샷) 양쪽에서 같은 모양으로 쓴다.
 */
export default function RecipeView({ recipe, ingredients, substitutes }) {
  if (!recipe) return null

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
          {ingredients?.map((item) => (
            <li key={item.name}>
              <span>{item.name}</span>
              <span className="muted">
                {item.quantity}
                {item.unit}
              </span>
            </li>
          ))}
        </ul>
      </section>

      <section>
        <h4 className="recipe__subtitle">만드는 법</h4>
        <ol className="recipe__steps">
          {recipe.steps?.map((step, index) => (
            // 조리 순서는 순서 자체가 의미라 index를 key로 써도 안전하다.
            // 중간에 추가/삭제되지 않는 정적 목록이다.
            <li key={index}>{step}</li>
          ))}
        </ol>
      </section>

      {substitutes?.length > 0 && (
        <section>
          <h4 className="recipe__subtitle">대체 재료</h4>
          <ul className="recipe__substitutes">
            {substitutes.map((sub) => (
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
