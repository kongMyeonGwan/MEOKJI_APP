import { formatMinutes } from '../../utils/format'
import '../../styles/components/recipe/RecipeView.css'

/**
 * 레시피 상세 표시.
 * 추천 결과(아직 저장 전)와 History(저장된 스냅샷) 양쪽에서 같은 모양으로 쓴다.
 *
 * 시세가 없는 재료를 재료 목록에서 밝히는 이유
 * --------------------------------------------
 * 아래 가격표(PriceTable)는 KAMIS 조사가격을 쓰는데, KAMIS 는 농수산물만
 * 조사한다. 두부 · 밀가루 · 간장 같은 가공식품은 값이 아예 없다.
 *
 * 표 아래에 한 줄로 적어두긴 했지만 "어느 재료가 빠졌는지" 는 알 수 없었다.
 * 재료비 합계가 실제보다 적게 보이는데 왜 그런지 모르는 상태가 된다.
 * 그래서 재료 줄마다 표시한다 — 표를 안 내려봐도 바로 보인다.
 */
export default function RecipeView({ recipe, ingredients, substitutes, prices }) {
  if (!recipe) return null

  // 조사 대상이 아닌 재료 이름.
  //
  // "값이 안 왔다" 가 아니라 "앞으로도 값이 없다" 만 표시한다. 서버가
  // surveyed=false 로 알려주는데, 이건 KAMIS 품목표만 보고 정한 값이라
  // 호출 제한이나 통신 오류로 그날 값이 비어도 흔들리지 않는다.
  // 일시적인 실패까지 "조사 대상이 아님" 이라고 적으면 거짓말이 된다.
  //
  // prices 를 안 넘기는 화면(History 의 저장된 스냅샷)도 있다. 그때는
  // 빈 집합이라 표시가 안 붙는다 — 지난 기록에 지금 시세를 말할 이유가 없다.
  const unsurveyed = new Set()
  for (const item of prices ?? []) {
    if (item.surveyed === false) {
      // 이름이 "두부 1모" 처럼 분량까지 붙어 오므로 앞부분만 쓴다
      unsurveyed.add(item.name.split(' ')[0])
    }
  }

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
              <span>
                {item.name}
                {unsurveyed.has(item.name) && (
                  <span className="recipe__no-price" title="농수산물 시세 조사 대상이 아닙니다">
                    조사 대상 아님
                  </span>
                )}
              </span>
              <span className="muted">
                {item.quantity}
                {item.unit}
              </span>
            </li>
          ))}
        </ul>

        {unsurveyed.size > 0 && (
          <p className="recipe__note">
            <strong>특이사항</strong>
            <span className="recipe__note-names">
              {[...unsurveyed].join(' · ')}
            </span>
            농수산물 시세 조사 대상이 아닌 재료입니다. 정부가 조사해서
            공개하는 값은 농산물 · 축산물 · 수산물이고, 두부 · 밀가루 · 간장 ·
            면류 같은 가공식품은 여기 들어가지 않아 공개 시세가 없습니다.
            그래서 아래 재료비 합계에서도 빠져 있으니, 실제로 장을 보면
            그만큼 더 나온다고 보시면 됩니다.
          </p>
        )}
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
