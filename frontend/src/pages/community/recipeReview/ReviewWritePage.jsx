import { Link } from 'react-router-dom'
import { SAMPLE_PHOTO, SAMPLE_WRITE_HISTORIES } from '../../../data/sampleBoard'

// 글쓰기 화면 (/community/write).
// 글은 내 식사 기록(History) 하나를 골라서 쓴다. 사진은 선택.
//
// 퍼블리싱만 된 화면이다. 입력칸과 버튼 모양만 있고 등록은 아직 아무 일도 하지 않는다.
// 드롭다운과 사진 미리보기는 가데이터다.
//
// TODO(게시판 담당)
//   - 드롭다운을 getHistories({ shared: false }) 로 받아온다 (아직 공유 안 한 내 기록)
//   - 제목 · 기록 · 내용이 비었는지 검사한다
//   - 고른 사진을 미리보기로 바꿔 보여준다 (5MB 이하 이미지만)
//   - 등록하면 createReview → uploadReviewFile 순서로 보내고 쓴 글로 이동한다
export default function ReviewWritePage() {
  const histories = SAMPLE_WRITE_HISTORIES

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

      <div>
        <label className="write__field">
          <span>제목</span>
          <input type="text" placeholder="어떤 이야기인가요?" maxLength={200} />
        </label>

        <label className="write__field">
          <span>공유할 내 기록</span>
          <select defaultValue="">
            <option value="">기록을 선택해주세요</option>
            {histories.map((history) => (
              <option key={history.id} value={history.id}>
                {history.food_name} — {history.recipe_name}
              </option>
            ))}
          </select>
          <p className="muted write__help">선택한 기록의 레시피와 재료가 게시글에 함께 표시됩니다.</p>
        </label>

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
    </div>
  )
}
