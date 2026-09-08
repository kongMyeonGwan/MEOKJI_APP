import { formatPrice } from '../../utils/format'
import '../../styles/components/price/PriceTable.css'

/**
 * 재료별 최저가 / 최고가 표.
 *
 * 네이버쇼핑·쿠팡·알리익스프레스에서 같은 재료의 가격을 모아 온 뒤
 * 가장 싼 곳과 가장 비싼 곳을 나란히 보여준다.
 * 가격은 DB에 저장하지 않는다. 조회 시점의 값이다.
 */

/** offers 배열에서 최저/최고를 뽑는다. */
function pickRange(offers) {
  if (!offers?.length) return null
  // 원본 배열을 정렬하면 호출한 쪽의 데이터가 바뀐다. 복사본을 만든다.
  const sorted = [...offers].sort((a, b) => a.price - b.price)
  return { low: sorted[0], high: sorted[sorted.length - 1] }
}

export function getPriceSummary(prices) {
  // 재료마다 최저가끼리 더한 값과 최고가끼리 더한 값을 각각 구한다.
  //   min: 전부 제일 싼 데서 샀을 때
  //   max: 전부 제일 비싼 데서 샀을 때
  let min = 0
  let max = 0

  for (const item of prices) {
    const range = pickRange(item.offers)

    // 판매처 정보가 없는 재료는 건너뛴다
    if (!range) continue

    min += range.low.price
    max += range.high.price
  }

  return { min, max }
}

export default function PriceTable({ prices }) {
  if (!prices?.length) return null

  const total = getPriceSummary(prices)

  return (
    <>
      <div className="table-wrap">
        <table className="table price-table">
          <thead>
            <tr>
              <th>재료</th>
              <th style={{ textAlign: 'right' }}>최저가</th>
              <th style={{ textAlign: 'right' }}>최고가</th>
            </tr>
          </thead>
          <tbody>
            {prices.map((item) => {
              const range = pickRange(item.offers)
              if (!range) return null

              // 파는 곳이 한 곳뿐이면 최저=최고라 비교가 의미 없다
              const single = item.offers.length === 1

              return (
                <tr key={item.name}>
                  <td>{item.name}</td>

                  <td className="num">
                    <a
                      href={range.low.link}
                      target="_blank"
                      rel="noreferrer noopener"
                      className="price-table__best"
                    >
                      {formatPrice(range.low.price)}
                    </a>
                    <span className="price-table__shop">{range.low.shop}</span>
                  </td>

                  <td className="num">
                    {single ? (
                      <span className="muted">-</span>
                    ) : (
                      <>
                        <span className="price-table__worst">
                          {formatPrice(range.high.price)}
                        </span>
                        <span className="price-table__shop">{range.high.shop}</span>
                      </>
                    )}
                  </td>
                </tr>
              )
            })}

            <tr className="price-table__total">
              <td>
                <strong>합계</strong>
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
        네이버쇼핑 · 쿠팡 · 알리익스프레스에서 조회한 값입니다. 배송비는 포함되지
        않으며, 실제 구매가와 다를 수 있습니다.
      </p>
    </>
  )
}
