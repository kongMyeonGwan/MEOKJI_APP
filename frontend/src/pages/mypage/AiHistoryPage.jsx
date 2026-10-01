import { useState } from 'react'
import { Link } from 'react-router-dom'
import { clearLogs, getLogs } from '../../utils/aiLog'
import { formatDateTime } from '../../utils/format'
import '../../styles/ListPage.css'
import './AiHistoryPage.css'

// AI 추천 기록. 언제, 어떤 조건으로, 무엇을 추천받았는지.
// 서버가 아니라 이 브라우저(localStorage)에 저장된 기록이다. (utils/aiLog.js)
export default function AiHistoryPage() {
  const [logs, setLogs] = useState(getLogs())

  const handleClear = () => {
    if (window.confirm(`추천 기록 ${logs.length}건을 모두 지울까요?`)) {
      clearLogs()
      setLogs([])
    }
  }

  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">AI 추천 기록</h2>
        {logs.length > 0 && (
          <button type="button" className="section-more" onClick={handleClear}>
            전체 지우기
          </button>
        )}
      </div>

      <p className="muted list-page__desc">
        어떤 조건으로 무엇을 추천받았는지 이 브라우저에 남겨둡니다. 고른 메뉴는 History에도 함께 기록돼요.
      </p>

      {logs.length === 0 && (
        <p className="empty">
          아직 추천받은 기록이 없어요.
          <br />
          <br />
          <Link to="/" className="btn btn--outline btn--sm">
            오늘의 추천 보러 가기
          </Link>
        </p>
      )}

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
                  <Link to={`/recipe/${name}`} className={name === log.picked ? 'ailog__food is-picked' : 'ailog__food'}>
                    {name === log.picked && '✓ '}
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
    </div>
  )
}
