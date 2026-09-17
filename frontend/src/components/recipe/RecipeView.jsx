import { formatMinutes } from '../../utils/format'
import '../../styles/components/recipe/RecipeView.css'

// 레시피 상세를 보여주는 컴포넌트.
// 추천 결과(아직 저장 전)랑 History(저장된 것) 양쪽에서 같이 쓴다.
//
// 아래 가격표는 KAMIS 조사가격을 쓰는데 KAMIS 는 농수산물만 조사한다.
// 두부, 밀가루, 간장 같은 가공식품은 값이 아예 없다.
// 그래서 어떤 재료에 값이 없는지 재료 줄마다 표시해준다.
export default function RecipeView({ recipe, ingredients, substitutes, prices }) {
  if (!recipe) {
    return null
  }

  // 시세 조사 대상이 아닌 재료 이름을 모은다.
  //
  // "값이 안 왔다" 가 아니라 "앞으로도 값이 없다" 만 표시한다.
  // 서버가 surveyed: false 로 알려주는데 이건 품목표를 보고 정한 값이라
  // 통신 오류로 그날 값이 비어도 안 흔들린다.
  //
  // prices 를 안 넘기는 화면(History)도 있다. 그때는 빈 배열이라 표시가 안 붙는다.
  const unsurveyed = []
  let priceList = prices
  if (!priceList) {
    priceList = []
  }

  for (let i = 0; i < priceList.length; i++) {
    const item = priceList[i]

    if (item.surveyed === false) {
      // 이름이 "두부 1모" 처럼 분량까지 붙어 와서 앞부분만 쓴다
      const name = item.name.split(' ')[0]
      if (!unsurveyed.includes(name)) {
        unsurveyed.push(name)
      }
    }
  }

  let ingredientList = ingredients
  if (!ingredientList) {
    ingredientList = []
  }

  let stepList = recipe.steps
  if (!stepList) {
    stepList = []
  }

  let substituteList = substitutes
  if (!substituteList) {
    substituteList = []
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
          {ingredientList.map(function (item) {
            return (
              <li key={item.name}>
                <span>
                  {item.name}
                  {unsurveyed.includes(item.name) && (
                    <span
                      className="recipe__no-price"
                      title="농수산물 시세 조사 대상이 아닙니다"
                    >
                      조사 대상 아님
                    </span>
                  )}
                </span>
                <span className="muted">
                  {item.quantity}
                  {item.unit}
                </span>
              </li>
            )
          })}
        </ul>

        {unsurveyed.length > 0 && (
          <p className="recipe__note">
            <strong>특이사항</strong>
            <span className="recipe__note-names">{unsurveyed.join(' · ')}</span>
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
          {stepList.map(function (step, index) {
            // 조리 순서는 중간에 바뀌지 않는 목록이라 index 를 key 로 써도 된다
            return <li key={index}>{step}</li>
          })}
        </ol>
      </section>

      {substituteList.length > 0 && (
        <section>
          <h4 className="recipe__subtitle">대체 재료</h4>
          <ul className="recipe__substitutes">
            {substituteList.map(function (sub) {
              return (
                <li key={sub.original}>
                  <strong>{sub.original}</strong> → {sub.substitute}
                  <span className="muted"> · {sub.reason}</span>
                </li>
              )
            })}
          </ul>
        </section>
      )}
    </div>
  )
}
