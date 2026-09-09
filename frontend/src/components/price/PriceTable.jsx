import { formatPrice } from '../../utils/format'
import '../../styles/components/price/PriceTable.css'

/**
 * 재료별 최저가 / 최고가 표.
 *
 * 같은 재료의 시장별 조사가격(KAMIS)을 모아 온 뒤
 * 가장 싼 곳과 가장 비싼 곳을 나란히 보여준다.
 * 가격은 DB에 저장하지 않는다. 조회 시점의 값이다.
 */

/** "20260908" -> "9/8". 조사일자는 연도까지 보여줄 만큼 중요하진 않다. */
function formatSurveyDate(text) {
  if (!text || text.length !== 8) return null
  return `${Number(text.slice(4, 6))}/${Number(text.slice(6, 8))}`
}

/** offers 중 가장 최근 조사일자를 찾는다. 없으면 null. */
function pickSurveyDate(prices) {
  const days = prices.flatMap((item) =>
    (item.offers ?? []).map((o) => o.surveyed_at).filter(Boolean),
  )
  if (!days.length) return null
  return formatSurveyDate(days.reduce((a, b) => (a > b ? a : b)))
}

/** offers 배열에서 최저/최고를 뽑는다. */
function pickRange(offers) {
  if (!offers?.length) return null
  // 원본 배열을 정렬하면 호출한 쪽의 데이터가 바뀐다. 복사본을 만든다.
  const sorted = [...offers].sort((a, b) => a.price - b.price)
  return { low: sorted[0], high: sorted[sorted.length - 1] }
}

export function getPriceSummary(prices) {
  // 재료마다 최저가끼리 더한 값과 최고가끼리 더한 값을 각각 구한다.
  //   min: 전부 제일 싼 시장에서 샀을 때
  //   max: 전부 제일 비싼 시장에서 샀을 때
  //
  // 더하는 건 판매 단위 가격이 아니라 **필요한 만큼의 값**(low_cost)이다.
  // KAMIS 는 "고춧가루 1kg 25,200원" 으로 값을 주는데 레시피가 요구하는 건
  // 1큰술이라, 단위 가격을 그대로 더하면 재료비가 백 배 넘게 부풀려진다.
  let min = 0
  let max = 0

  // 합계에 못 들어간 재료가 있으면 몇 개를 더한 값인지 밝힌다.
  // 안 그러면 재료 15개짜리 레시피의 합계가 5개 값인 줄 모르고
  // "이 요리 전체 비용"으로 읽힌다.
  //
  // 빠지는 이유는 두 가지다.
  //   가격 없음    KAMIS 가 조사하지 않는 가공식품 (두부 · 간장)
  //   환산 불가    판매 단위와 레시피 단위를 맞출 수 없음
  let counted = 0

  for (const item of prices) {
    if (item.low_cost == null || item.high_cost == null) continue

    min += item.low_cost
    max += item.high_cost
    counted += 1
  }

  return { min, max, counted, total: prices.length }
}

export default function PriceTable({ prices }) {
  if (!prices?.length) return null

  const total = getPriceSummary(prices)
  const surveyedAt = pickSurveyDate(prices)

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
            {prices.map((item) => {
              const range = pickRange(item.offers)

              // 가격을 못 구한 재료도 줄은 남긴다.
              // 줄째로 빼버리면 "이 재료는 왜 없지?" 가 되고,
              // 합계가 전체 재료비인 것처럼 보인다.
              if (!range) {
                return (
                  <tr key={item.name} className="price-table__unknown">
                    <td>{item.name}</td>
                    <td className="num muted" colSpan={2}>
                      가격 정보 없음
                    </td>
                  </tr>
                )
              }

              // 판매 단위와 레시피 단위를 못 맞춘 경우.
              // 단위 가격은 보여주되 합계에는 넣지 않는다.
              const scaled = item.low_cost != null && item.high_cost != null

              // 조사처가 한 곳뿐이면 최저=최고라 비교가 의미 없다
              const single = item.offers.length === 1

              return (
                <tr key={item.name} className={scaled ? undefined : 'price-table__unknown'}>
                  <td>{item.name}</td>

                  <td className="num">
                    {scaled ? (
                      <>
                        <a
                          href={range.low.link}
                          target="_blank"
                          rel="noreferrer noopener"
                          className="price-table__best"
                        >
                          {formatPrice(item.low_cost)}
                        </a>
                        <span className="price-table__shop">
                          {range.low.shop} · {range.low.unit} {formatPrice(range.low.price)}
                        </span>
                      </>
                    ) : (
                      <>
                        <span className="muted">환산 불가</span>
                        <span className="price-table__shop">
                          {range.low.unit} {formatPrice(range.low.price)}
                        </span>
                      </>
                    )}
                  </td>

                  <td className="num">
                    {single || !scaled ? (
                      <span className="muted">-</span>
                    ) : (
                      <>
                        <span className="price-table__worst">
                          {formatPrice(item.high_cost)}
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
                {total.counted < total.total && (
                  <span className="price-table__unit">
                    재료 {total.total}개 중 {total.counted}개
                  </span>
                )}
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
        {surveyedAt
          ? `${surveyedAt} 조사된 시장별 소매가격입니다(KAMIS). `
          : '시장별 소매가격입니다(KAMIS). '}
        금액은 <strong>이 레시피에 필요한 만큼</strong>으로 환산한 값이고, 회색
        글씨가 시장의 판매 단위 가격입니다. 실제로는 그 단위로 사야 하므로
        구매 금액은 더 큽니다.
        {total.counted < total.total &&
          ' 가공식품은 농수산물 시세 조사 대상이 아니라 가격이 없습니다.'}
      </p>
    </>
  )
}
