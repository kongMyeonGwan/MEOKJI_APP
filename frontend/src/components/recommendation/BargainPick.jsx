import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import FoodThumb from '../common/FoodThumb'
import Loading from '../common/Loading'
import { getRecommendations } from '../../api/recommendations'
import { useAiLog } from '../../contexts/AiLogContext'
import { formatPrice } from '../../utils/format'
import '../../styles/components/recommendation/BargainPick.css'

/**
 * "지금 싼 재료로" — 시세를 보고 고른 메뉴.
 *
 * 날씨 추천(WeatherPick)이 "오늘 어떤 날씨인가" 로 고른다면,
 * 이 카드는 "요즘 뭐가 싼가" 로 고른다. 축이 다르다.
 *
 * 왜 이게 되는가
 * --------------
 * 쇼핑몰 API 는 "지금 얼마" 만 준다. 어제보다 싼지 비싼지는 알 수 없다.
 * KAMIS 는 일별 조사가격이라 흐름을 볼 수 있고, 그래서 "요즘 값이 내린
 * 재료" 를 뽑아 AI 에게 넘길 수 있다.
 *
 * 화면 구성
 * ---------
 *   위   싼 재료 칩 — 왜 이 메뉴들인지에 대한 근거
 *   아래 그 재료를 쓰는 메뉴 카드
 *
 * 근거를 위에 두는 이유는, 근거 없이 메뉴만 있으면 "왜 이걸 주지?" 가
 * 되기 때문이다. 날씨 카드가 온도를 먼저 보여주는 것과 같은 이유다.
 */

/** 한 번에 받아올 메뉴 개수 */
const PAGE_SIZE = 4

export default function BargainPick() {
  const { add } = useAiLog()

  const [items, setItems] = useState([])
  const [bargains, setBargains] = useState([])
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    let alive = true

    getRecommendations({ source: '시세 추천', useBargains: true, limit: PAGE_SIZE })
      .then((data) => {
        if (!alive) return

        setItems(data.items ?? [])
        setBargains(data.bargains ?? [])

        if (data.items?.length) {
          add({
            source: '시세 추천',
            condition: data.condition,
            foods: data.items.map((food) => food.food_name),
          })
        }
      })
      .catch(() => {
        // 시세를 못 받아도 대시보드의 다른 칸은 멀쩡해야 한다.
        // 에러 문구를 크게 띄우는 대신 카드를 통째로 숨긴다.
        if (alive) setFailed(true)
      })
      .finally(() => {
        if (alive) setLoading(false)
      })

    return () => {
      alive = false
    }
    // add 는 매 렌더마다 새로 만들어져서 넣으면 무한 호출이 된다
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  if (failed) return null

  if (loading) {
    return (
      <section className="card bargain">
        <h2 className="section-title">지금 싼 재료로</h2>
        <Loading
          steps={['요즘 시세를 살펴보는 중...', 'AI가 메뉴를 고르는 중...']}
        />
      </section>
    )
  }

  // 시세가 잔잔한 날은 싼 재료가 없다. 그때는 카드를 띄우지 않는다 —
  // "지금 싼 재료로" 라고 해놓고 아무것도 없으면 이상하다.
  if (!bargains.length || !items.length) return null

  return (
    <section className="card bargain">
      <header className="bargain__head">
        <h2 className="section-title">지금 싼 재료로</h2>
        <p className="muted bargain__sub">일주일 전보다 값이 내렸어요</p>
      </header>

      <ul className="bargain__chips">
        {bargains.map((row) => (
          <li key={row.name} className="bargain__chip">
            <strong>{row.name}</strong>
            <span className="bargain__drop">{Math.abs(row.percent).toFixed(0)}%↓</span>
            <span className="bargain__price">
              {formatPrice(row.price)}/{row.unit}
            </span>
          </li>
        ))}
      </ul>

      <ul className="bargain__foods">
        {items.map((food) => (
          <li key={food.food_name}>
            <Link
              to={`/recipe/${encodeURIComponent(food.food_name)}`}
              className="bargain__food"
            >
              <FoodThumb item={food} size="sm" />
              <span className="bargain__food-body">
                <strong className="bargain__food-name">{food.food_name}</strong>
                <span className="muted bargain__food-reason">{food.reason}</span>
              </span>
            </Link>
          </li>
        ))}
      </ul>

      <p className="muted bargain__note">
        한국농수산식품유통공사(KAMIS)가 조사한 전국 소매가격 기준입니다.
      </p>
    </section>
  )
}
