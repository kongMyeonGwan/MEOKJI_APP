import { formatPrice } from '../../utils/format'
import '../../styles/components/price/PriceTable.css'

// 재료별 최저가 / 최고가 표.
// 같은 재료의 시장별 조사가격(KAMIS)을 받아와서 제일 싼 곳이랑
// 제일 비싼 곳을 나란히 보여준다.
// 가격은 DB 에 저장 안 한다. 조회한 시점의 값이다.

// "20260908" -> "9/8". 조사일자는 연도까지 보여줄 만큼 중요하지 않다.
function formatSurveyDate(text) {
  if (!text || text.length !== 8) {
    return null
  }

  const month = Number(text.slice(4, 6))
  const day = Number(text.slice(6, 8))
  return month + '/' + day
}

// 가장 최근 조사일자를 찾는다. 없으면 null.
function pickSurveyDate(prices) {
  let latest = null

  for (let i = 0; i < prices.length; i++) {
    let offers = prices[i].offers
    if (!offers) {
      offers = []
    }

    for (let j = 0; j < offers.length; j++) {
      const day = offers[j].surveyed_at
      if (!day) {
        continue
      }
      if (latest === null || day > latest) {
        latest = day
      }
    }
  }

  if (latest === null) {
    return null
  }

  return formatSurveyDate(latest)
}

// 가격이 올랐는지 내렸는지 보여주는 작은 조각.
// 장 볼 때 "지금이 살 때인가" 를 알려준다.
function TrendBadge({ trend }) {
  if (!trend) {
    return null
  }

  const down = trend.percent < 0
  const percent = Math.abs(trend.percent).toFixed(0)

  let className = 'price-table__trend is-up'
  let arrow = '▲'
  if (down) {
    className = 'price-table__trend is-down'
    arrow = '▼'
  }

  const title =
    formatPrice(trend.from_price) +
    ' → ' +
    formatPrice(trend.to_price) +
    ' (같은 단위 전국 평균)'

  return (
    <span className={className} title={title}>
      {arrow} {percent}%
    </span>
  )
}

// offers 에서 제일 싼 것과 제일 비싼 것을 뽑는다.
function pickRange(offers) {
  if (!offers || offers.length === 0) {
    return null
  }

  // 원본 배열을 정렬하면 부른 쪽 데이터가 바뀐다. 복사본을 만든다.
  const sorted = offers.slice()
  sorted.sort(function (a, b) {
    return a.price - b.price
  })

  return { low: sorted[0], high: sorted[sorted.length - 1] }
}

export function getPriceSummary(prices) {
  // 재료마다 최저가끼리 더한 값이랑 최고가끼리 더한 값을 구한다.
  //   min : 전부 제일 싼 시장에서 샀을 때
  //   max : 전부 제일 비싼 시장에서 샀을 때
  //
  // 더하는 건 판매 단위 가격이 아니라 실제로 내는 돈(low_cost)이다.
  // KAMIS 는 "고춧가루 1kg 25,200원" 으로 주는데 레시피가 필요한 건
  // 1큰술이라, 그대로 더하면 재료비가 백 배 넘게 부풀려진다.
  let min = 0
  let max = 0

  // 합계에 못 들어간 재료가 있으면 몇 개를 더한 값인지 밝힌다.
  // 안 그러면 재료 15개짜리 레시피의 합계가 5개 값인 줄 모른다.
  //
  // 빠지는 이유는 두 가지다.
  //   가격 없음  - KAMIS 가 조사 안 하는 가공식품 (두부, 간장)
  //   환산 불가  - 판매 단위랑 레시피 단위를 못 맞춤
  let counted = 0

  // 필요한 양보다 더 사게 된 재료 수.
  // 합계가 왜 레시피 분량보다 큰지 표 아래에서 설명하려고 센다.
  let minUnit = 0

  for (let i = 0; i < prices.length; i++) {
    const item = prices[i]

    if (item.low_cost === null || item.low_cost === undefined) {
      continue
    }
    if (item.high_cost === null || item.high_cost === undefined) {
      continue
    }

    min = min + item.low_cost
    max = max + item.high_cost
    counted = counted + 1

    if (item.min_unit) {
      minUnit = minUnit + 1
    }
  }

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

  // 조사 대상이 아닌 재료 이름. 표 아래 안내에 이름을 밝히려고 모은다.
  // "값이 안 왔다" 랑 섞으면 안 된다. 통신 오류로 값이 빈 재료까지
  // "가공식품이라 없다" 고 말하게 된다.
  const unsurveyed = []
  for (let i = 0; i < prices.length; i++) {
    if (prices[i].surveyed === false) {
      unsurveyed.push(prices[i].name)
    }
  }

  const surveyedAt = pickSurveyDate(prices)

  let noteHead = '시장별 소매가격입니다(KAMIS). '
  if (surveyedAt) {
    noteHead = surveyedAt + ' 조사된 시장별 소매가격입니다(KAMIS). '
  }

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
            {prices.map(function (item) {
              const range = pickRange(item.offers)

              // 가격을 못 구한 재료도 줄은 남긴다.
              // 줄째로 빼면 "이 재료는 왜 없지?" 가 되고
              // 합계가 전체 재료비인 것처럼 보인다.
              if (!range) {
                // 값이 없는 이유가 두 가지고 사용자한테는 다른 말이다.
                //   조사 대상 아님 - 두부, 간장 같은 가공식품. 앞으로도 값이 없다
                //   못 받음        - 통신 오류. 잠시 뒤엔 나온다
                let reason = '가격을 받지 못함'
                if (item.surveyed === false) {
                  reason = '조사 대상 아님'
                }

                return (
                  <tr key={item.name} className="price-table__unknown">
                    <td>{item.name}</td>
                    <td className="num muted" colSpan={2}>
                      {reason}
                    </td>
                  </tr>
                )
              }

              // 판매 단위랑 레시피 단위를 못 맞춘 경우.
              // 단위 가격은 보여주되 합계에는 안 넣는다.
              let scaled = true
              if (item.low_cost === null || item.low_cost === undefined) {
                scaled = false
              }
              if (item.high_cost === null || item.high_cost === undefined) {
                scaled = false
              }

              // 조사한 곳이 한 군데뿐이면 최저 = 최고라 비교가 의미 없다
              const single = item.offers.length === 1

              let rowClass = undefined
              if (!scaled) {
                rowClass = 'price-table__unknown'
              }

              return (
                <tr key={item.name} className={rowClass}>
                  <td>
                    {item.name}
                    <TrendBadge trend={item.trend} />
                    {/* 쪼개 팔지 않아서 더 산 재료.
                        값이 부풀어 보이는 이유를 그 줄에서 바로 밝힌다. */}
                    {scaled && item.min_unit && (
                      <span
                        className="price-table__minunit"
                        title={
                          '이 레시피에 필요한 건 ' +
                          item.need_text +
                          '지만 ' +
                          item.buy_text +
                          ' 단위로만 팔아서, ' +
                          item.buy_text +
                          ' 값을 넣었습니다.'
                        }
                      >
                        최소단위
                      </span>
                    )}
                  </td>

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
                          {range.low.shop} · {range.low.unit}{' '}
                          {formatPrice(range.low.price)}
                        </span>
                        {item.min_unit && item.need_text && item.buy_text && (
                          <span className="price-table__shop">
                            {item.need_text} 필요 → {item.buy_text} 구입
                          </span>
                        )}
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
                        <span className="price-table__shop">
                          {range.high.shop}
                        </span>
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
                {total.minUnit > 0 && (
                  <span className="price-table__unit">
                    최소단위 구입 {total.minUnit}개 포함
                  </span>
                )}
              </td>
              <td className="num">
                <strong className="price-table__best">
                  {formatPrice(total.min)}
                </strong>
              </td>
              <td className="num">
                <strong>{formatPrice(total.max)}</strong>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <p className="muted price-table__note">
        {noteHead}
        금액은 <strong>실제로 사게 되는 만큼</strong>의 값이고, 회색 글씨가
        시장의 판매 단위 가격입니다.{' '}
        <strong className="price-table__minunit">최소단위</strong> 표시가 붙은
        재료는 <strong>최소단위로 구입</strong>한 값입니다 — 무 0.2개처럼
        레시피가 요구하는 양이 판매 단위보다 적어도 쪼개 팔지 않아 한 단위를
        통째로 사야 하므로, 필요한 양이 아니라 사야 하는 양으로 계산했습니다.
        쌀 · 고춧가루처럼 무게를 달아 파는 재료는 필요한 만큼만 삽니다.
        {unsurveyed.length > 0 && (
          <span>
            {' '}
            {unsurveyed.join(' · ')} — 농수산물 시세 조사 대상이 아니라 값이
            없고, 합계에서도 빠집니다.
          </span>
        )}{' '}
        <strong className="price-table__trend is-down">▼</strong>
        {' · '}
        <strong className="price-table__trend is-up">▲</strong>는 일주일 전 전국
        평균과 견준 값입니다.
      </p>
    </>
  )
}
