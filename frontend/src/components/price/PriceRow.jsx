import { formatPrice } from '../../utils/format'

// 가격표의 한 줄 (재료 하나). PriceTable 이 재료마다 하나씩 그린다.
//
// item 모양 (서버가 준다)
//   name       재료 이름
//   offers     시장별 가격 [{ shop: '○○마트', price: 3000, unit: '1kg', link: '...' }, ...]
//   low_cost   가장 싼 곳에서 레시피 분량만큼 샀을 때 내는 돈 (계산 못 하면 null)
//   high_cost  가장 비싼 곳에서 샀을 때 내는 돈 (계산 못 하면 null)
//   surveyed   false 면 시세 조사 대상이 아닌 재료 (두부, 간장 같은 가공식품)
//   min_unit   true 면 쪼개 팔지 않아서 필요한 양보다 많이 사야 하는 재료
//   trend      일주일 전보다 오르내린 비율 { percent: -12 } (없으면 null)
export default function PriceRow({ item }) {
  const offers = item.offers || []

  // 1. 가격 정보가 아예 없는 재료
  if (offers.length === 0) {
    return (
      <tr className="price-table__unknown">
        <td>{item.name}</td>
        <td className="num muted" colSpan={2}>
          {item.surveyed === false ? '조사 대상 아님' : '가격을 받지 못함'}
        </td>
      </tr>
    )
  }

  // 2. 제일 싼 곳(low)과 제일 비싼 곳(high)을 찾는다
  let low = offers[0]
  let high = offers[0]
  offers.forEach((offer) => {
    if (offer.price < low.price) {
      low = offer
    }
    if (offer.price > high.price) {
      high = offer
    }
  })

  // 3. 레시피 분량으로 계산이 됐는지 (단위를 못 맞추면 null 이 온다)
  const calculated = item.low_cost !== null && item.high_cost !== null

  // 4. 일주일 전보다 올랐는지 내렸는지
  let trendText = ''
  let trendClass = ''
  if (item.trend) {
    const percent = Math.abs(item.trend.percent).toFixed(0) // -12.3 → "12"
    if (item.trend.percent < 0) {
      trendText = `▼ ${percent}%`
      trendClass = 'price-table__trend is-down'
    } else {
      trendText = `▲ ${percent}%`
      trendClass = 'price-table__trend is-up'
    }
  }

  return (
    <tr className={calculated ? '' : 'price-table__unknown'}>
      {/* 재료 이름 칸 */}
      <td>
        {item.name}
        {item.trend && <span className={trendClass}>{trendText}</span>}
        {calculated && item.min_unit && <span className="price-table__minunit">최소단위</span>}
      </td>

      {/* 최저가 칸 */}
      <td className="num">
        {calculated ? (
          <>
            <a href={low.link} target="_blank" rel="noreferrer" className="price-table__best">
              {formatPrice(item.low_cost)}
            </a>
            <span className="price-table__shop">
              {low.shop} · {low.unit} {formatPrice(low.price)}
            </span>
          </>
        ) : (
          <>
            <span className="muted">환산 불가</span>
            <span className="price-table__shop">
              {low.unit} {formatPrice(low.price)}
            </span>
          </>
        )}
      </td>

      {/* 최고가 칸. 계산이 안 됐거나 조사한 곳이 한 군데뿐이면 - 로 둔다 */}
      <td className="num">
        {calculated && offers.length > 1 ? (
          <>
            <span className="price-table__worst">{formatPrice(item.high_cost)}</span>
            <span className="price-table__shop">{high.shop}</span>
          </>
        ) : (
          <span className="muted">-</span>
        )}
      </td>
    </tr>
  )
}
