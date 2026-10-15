import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import ErrorMessage from '../../../components/common/ErrorMessage'
import Loading from '../../../components/common/Loading'
import RecipeView from '../../../components/common/RecipeView'
import { getHistories } from '../../../api/histories'
import { formatDate, formatPrice } from '../../../utils/format'
import { SAMPLE_PHOTO } from '../../../data/sampleBoard'

// 글쓰기 화면 (/community/write).
// 글은 내 식사 기록(History) 하나를 골라서 쓴다. 사진은 선택.
//
// "공유할 내 기록" 드롭다운은 서버에서 받아온다 (아직 공유 안 한 내 기록).
// 내 History 화면의 "이 기록으로 글 쓰기" 로 들어오면 주소에 ?historyId=5 가 붙어 있어서
// 그 기록이 미리 골라져 있다. 기록을 고르면 레시피 · 재료가 아래에 미리 보인다.
//
// 나머지는 퍼블리싱만 돼 있다. 제목 · 내용 · 사진 칸은 모양만 있고 등록은 아직 아무 일도 하지 않는다.
// 사진 미리보기는 가데이터다.
//
// TODO(게시판 담당)
//   - 제목 · 기록 · 내용이 비었는지 검사한다 (기록은 selected 가 있는지로 본다)
//   - 고른 사진을 미리보기로 바꿔 보여준다 (5MB 이하 이미지만)
//   - 등록하면 createReview → uploadReviewFile 순서로 보내고 쓴 글로 이동한다
//     서버는 snake_case 로 받는다 → { meal_history_id: selected.id, title, content }

const CONTENT_MAX = 2000 // 본문 최대 글자 수

export default function ReviewWritePage() {
  // 주소의 ?historyId=5 (없으면 빈 문자열)
  const [searchParams] = useSearchParams()

  const [histories, setHistories] = useState([]) // 드롭다운에 보여줄 내 기록들
  const [historyId, setHistoryId] = useState(searchParams.get('historyId') || '') // 고른 기록 id. select 값이라 문자열
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // 아직 공유 안 한 내 식사 기록을 받아온다. 한 번에 최대 50개 (서버의 Page.MAX_SIZE)
  useEffect(() => {
    const load = async () => {
      try {
        const data = await getHistories({ shared: false, size: 50 })
        setHistories(data.items || [])
      } catch (err) {
        // 서버가 보낸 오류 문장을 그대로 보여준다.
        if (err.response) {
          // 입력값 검사에 걸리면 detail 이 배열로 온다. 예) [{ msg: '닉네임을 입력해주세요' }]
          const detail = err.response.data.detail
          if (Array.isArray(detail)) {
            setError(detail[0].msg)
          } else {
            setError(detail || '요청을 처리하지 못했습니다.')
          }
        } else if (err.request) {
          // 백엔드를 안 켰거나 30초가 지난 경우
          setError('서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.')
        } else {
          // 그 밖의 에러 (예: api/reviews.js 의 "게시판 기능은 아직 준비 중입니다.")
          setError(err.message)
        }
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  // 고른 기록 한 건. 목록에 없는 id 면 (이미 공유했거나 남의 기록) undefined
  const selected = histories.find((history) => String(history.id) === historyId)

  return (
    <div className="card write">
      <div className="write__head">
        <h2 className="section-title" style={{ margin: 0 }}>
          글쓰기
        </h2>
        <Link to="/community" className="muted write__cancel">
          취소
        </Link>
      </div>

      {loading && <Loading message="내 기록을 불러오는 중..." />}
      {error && <ErrorMessage message={error} />}

      {!loading && !error && histories.length === 0 && (
        <p className="muted write__help">
          공유할 수 있는 기록이 없습니다. 레시피에서 "이걸로 먹을래요" 를 누르면 여기서 글을 쓸 수 있어요.{' '}
          <Link to="/history">내 기록 보기</Link>
        </p>
      )}

      {!loading && !error && histories.length > 0 && (
        <div>
          <label className="write__field">
            <span>제목</span>
            <input type="text" placeholder="어떤 이야기인가요?" maxLength={200} />
          </label>

          <label className="write__field">
            <span>공유할 내 기록</span>
            <select value={selected ? historyId : ''} onChange={(e) => setHistoryId(e.target.value)}>
              <option value="">기록을 선택해주세요</option>
              {histories.map((history) => (
                <option key={history.id} value={history.id}>
                  {formatDate(history.selected_at)} · {history.food_name} — {history.recipe_name}
                </option>
              ))}
            </select>
            <p className="muted write__help">선택한 기록의 레시피와 재료가 게시글에 함께 표시됩니다.</p>
          </label>

          {/* 고른 기록 미리보기. 게시글 상세에도 이 레시피가 붙는다 */}
          {selected && (
            <div className="write__history">
              <p className="write__history-head">
                <strong>{selected.food_name}</strong>
                <span className="muted"> · {selected.recipe_name}</span>
                <span className="muted write__history-cost">{formatPrice(selected.estimated_cost)}</span>
              </p>
              <RecipeView
                recipe={selected.recipe_json}
                ingredients={selected.ingredients_json}
                substitutes={selected.substitutes_json}
              />
            </div>
          )}

          <label className="write__field">
            <span>내용</span>
            <textarea placeholder="만들면서 있었던 일, 팁, 후기를 자유롭게 적어주세요." rows={12} />
          </label>

          <label className="write__field">
            <span>사진 (선택)</span>
            <input type="file" accept=".jpg,.jpeg,.png,.gif,.webp" />
            <p className="muted write__help">한 장까지 올릴 수 있어요. 5MB 이하 이미지.</p>
            {/* 고른 사진이 이 자리에 보인다. 지금은 가데이터 그림이다 */}
            <img className="write__preview" src={SAMPLE_PHOTO} alt="올릴 사진 미리보기" />
          </label>

          <div className="write__actions">
            <button type="button" className="btn">
              등록
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
