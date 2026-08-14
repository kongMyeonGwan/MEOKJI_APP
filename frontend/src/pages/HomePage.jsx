import ApiStatus from '../components/common/ApiStatus'

/**
 * Dashboard. 별도 Landing Page 없이 접속하자마자 이 화면이 보인다(§54).
 * 섹션 골격만 잡아두고, 각 섹션은 이후 STEP에서 실제 컴포넌트로 교체한다.
 */
export default function HomePage() {
  return (
    <div style={{ display: 'grid', gap: 20 }}>
      <ApiStatus />

      <section className="card">
        <h2 style={{ margin: 0, fontSize: 18 }}>현재 날씨</h2>
        <p className="muted">STEP 12에서 WeatherCard로 교체</p>
      </section>

      <section className="card" style={{ textAlign: 'center', padding: '32px 20px' }}>
        <h1 style={{ margin: '0 0 16px', fontSize: 28 }}>오늘 뭐 먹지?</h1>
        <p className="muted" style={{ margin: 0 }}>
          STEP 11에서 FoodSearch(직접 검색)로 교체
        </p>
      </section>

      <section className="card">
        <h2 style={{ margin: 0, fontSize: 18 }}>오늘 날씨 추천</h2>
        <p className="muted">STEP 13에서 RecommendationForm + 추천 카드로 교체</p>
      </section>

      <section className="card">
        <h2 style={{ margin: 0, fontSize: 18 }}>인기 게시글</h2>
        <p className="muted">STEP 20에서 PostCard 목록으로 교체</p>
      </section>
    </div>
  )
}
