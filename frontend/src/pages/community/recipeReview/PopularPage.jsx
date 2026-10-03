import DateRangeFilter from '../../../components/common/DateRangeFilter'
import LikedList from '../../../components/common/LikedList'
import { SAMPLE_POPULAR } from '../../../data/sampleBoard'

// 인기 레시피 전체 목록 + 기간 검색.
//
// 퍼블리싱만 된 화면이다. 가데이터를 그리고, 기간을 바꿔 '검색' 을 눌러도
// 목록은 그대로다.
//
// TODO(게시판 담당)
//   - start · end 를 useState 로 들고, DateRangeFilter 의 onSearch 로 받아 넣는다
//   - useEffect 에서 getPopular({ start, end }) 를 부른다 (의존성 배열에 [start, end])
export default function PopularPage() {
  const items = SAMPLE_POPULAR

  // 가데이터 기간 (최근 30일 모양)
  const start = '2026-09-05'
  const end = '2026-10-04'

  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">인기 레시피</h2>
        <span className="muted section-more">{items.length}건</span>
      </div>

      <p className="muted list-page__desc">선택한 기간 동안 좋아요를 많이 받은 순서입니다.</p>

      <div className="card">
        {/* onSearch 는 여러 화면이 같이 쓰는 DateRangeFilter 가 요구하는 값이다.
            기능 담당이 기간 state 를 넣는 자리다 */}
        <DateRangeFilter start={start} end={end} onSearch={() => {}} />
        <LikedList items={items} />
      </div>
    </div>
  )
}
