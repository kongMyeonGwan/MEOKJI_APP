import { Link } from 'react-router-dom'
import { useAiLog } from '../contexts/AiLogContext'
import { formatDateTime } from '../utils/format'
import '../styles/pages/ListPage.css'
import '../styles/pages/AiHistoryPage.css'

/**
 * AI 추천 기록.
 *
 * History(/history)와 다른 화면이다.
 *   History     내가 **먹기로 정한** 한 끼. 레시피와 재료가 통째로 남는다
 *   AI 추천 기록  AI에게 **물어본** 내역. 그때 어떤 조건으로 뭘 받았는지가 남는다
 *
 * 추천받고 아무것도 안 고른 날이 훨씬 많은데, 그 기록이 어디에도 안 남으면
 * "저번에 나왔던 그 메뉴"를 다시 찾을 방법이 없다. 그래서 고른 게 없어도 남긴다.
 *
 * 추천 결과 자체를 저장하지 않는다는 원칙(§13)과 어긋나지 않는다.
 * 여기 남는 건 메뉴 이름과 조건뿐이고, 레시피 본문은 다시 열 때 새로 만든다.
 *
 * 서버가 아니라 이 브라우저에 저장한다(AiLogContext).
 * 그래서 브라우저를 바꾸면 기록이 따라오지 않는다. 그 대신 테이블을
 * 하나도 늘리지 않았다.
 */
export default function AiHistoryPage() {
  const { logs, count, clear } = useAiLog()

  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">AI 추천 기록</h2>
        {count > 0 && (
          <button
            type="button"
            className="section-more"
            onClick={() => {
              // 한 번 지우면 되돌릴 수 없어서 먼저 물어본다
              if (window.confirm(`추천 기록 ${count}건을 모두 지울까요?`)) {
                clear()
              }
            }}
          >
            전체 지우기
          </button>
        )}
      </div>

      <p className="muted list-page__desc">
        어떤 조건으로 무엇을 추천받았는지 이 브라우저에 남겨둡니다. 고른 메뉴는
        History에도 함께 기록돼요.
      </p>

      {count === 0 ? (
        <p className="empty">
          아직 추천받은 기록이 없어요.
          <br />
          <br />
          <Link to="/" className="btn btn--outline btn--sm">
            오늘의 추천 보러 가기
          </Link>
        </p>
      ) : (
        <ul className="ailog">
          {logs.map((log) => (
            <li key={log.id} className="card ailog__item">
              <div className="ailog__head">
                <span className="tag ailog__source">{log.source}</span>
                <span className="muted ailog__date">{formatDateTime(log.created_at)}</span>
              </div>

              <p className="ailog__condition">{log.condition}</p>

              <ul className="ailog__foods">
                {log.foods.map((name) => (
                  <li key={name}>
                    <Link
                      to={`/recipe/${encodeURIComponent(name)}`}
                      className={`ailog__food${name === log.picked ? ' is-picked' : ''}`}
                    >
                      {/* 고른 메뉴에만 표시를 단다. 목록이 길어도 결론이 먼저 보인다 */}
                      {name === log.picked && <span aria-hidden="true">✓ </span>}
                      {name}
                    </Link>
                  </li>
                ))}
              </ul>

              <p className="muted ailog__result">
                {log.picked ? `이 중 '${log.picked}'(으)로 정하셨어요.` : '고른 메뉴 없음'}
              </p>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
