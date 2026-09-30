import PriceRow from './PriceRow'
import { formatPrice } from '../../utils/format'
import '../../styles/components/price/PriceTable.css'

// 재료별 최저가 / 최고가 표 (정부 KAMIS 시장 조사가격). 레시피 화면에서 쓴다.
// 한 줄 한 줄은 PriceRow.jsx 가 그린다.

// 재료비 합계를 구한다. RecipePage 에서도 가져다 쓴다.
//   min     : 전부 제일 싼 곳에서 샀을 때
//   max     : 전부 제일 비싼 곳에서 샀을 때
//   counted : 합계에 들어간 재료 수 (가격을 모르는 재료는 빠진다)
//   total   : 전체 재료 수
//   minUnit : 최소단위로 사야 하는 재료 수
export const getPriceSummary = (prices) => {
  let min = 0
  let max = 0
  let counted = 0
  let minUnit = 0

  prices.forEach((item) => {
    // 가격 계산이 안 된 재료는 건너뛴다
    if (item.low_cost === null || item.high_cost === null) {
      return
    }

    min = min + item.low_cost
    max = max + item.high_cost
    counted = counted + 1

    if (item.min_unit) {
      minUnit = minUnit + 1
    }
  })

  return {
    min: min,
    max: max,
    counted: counted,
    total: prices.length,
    minUnit: minUnit,
  }
}

export default function PriceTable({ prices }) {
  if (!prices || prices.length === 0) {
    return null
  }

  const total = getPriceSummary(prices)

  // 시세 조사 대상이 아닌 재료 이름들 (예: ['두부', '간장'])
  const unsurveyed = prices.filter((item) => item.surveyed === false).map((item) => item.name)

  return (
    <>
      <div className="table-wrap">
        <table className="table price-table">
          <thead>
            <tr>
              <th>재료</th>
              <th style={{ textAlign: 'right' }}>최저</th>
              <th style={{ textAlign: 'right' }}>최고</th>
            </tr>
          </thead>

          <tbody>
            {/* 재료마다 한 줄 */}
            {prices.map((item) => (
              <PriceRow key={item.name} item={item} />
            ))}

            {/* 마지막 줄: 합계 */}
            <tr className="price-table__total">
              <td>
                <strong>합계</strong>
                {total.counted < total.total && (
                  <span className="price-table__unit">
                    재료 {total.total}개 중 {total.counted}개
                  </span>
                )}
                {total.minUnit > 0 && <span className="price-table__unit">최소단위 구입 {total.minUnit}개 포함</span>}
              </td>
              <td className="num">
                <strong className="price-table__best">{formatPrice(total.min)}</strong>
              </td>
              <td className="num">
                <strong>{formatPrice(total.max)}</strong>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <p className="muted price-table__note">
        시장별 소매가격입니다(KAMIS). 금액은 레시피 분량만큼 실제로 사게 되는 값이고, 회색 글씨는 시장의 판매 단위
        가격입니다. <strong className="price-table__minunit">최소단위</strong> 표시가 붙은 재료는 쪼개 팔지 않아서 한
        단위를 통째로 산 값입니다.
        {unsurveyed.length > 0 && ` ${unsurveyed.join(' · ')} 은(는) 시세 조사 대상이 아니라 합계에서 빠집니다.`} ▼ ▲ 는
        일주일 전 전국 평균과 비교한 값입니다.
      </p>
    </>
  )
}
