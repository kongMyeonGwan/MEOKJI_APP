import { useEffect, useState } from 'react'
import { fetchHealth } from '../../api/health'
import Loading from './Loading'
import ErrorMessage from './ErrorMessage'

/**
 * 백엔드/DB 연결 상태 표시.
 * STEP 4에서 프론트↔백엔드 연결을 눈으로 확인하기 위한 개발용 카드.
 */
export default function ApiStatus() {
  const [health, setHealth] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchHealth()
      .then(setHealth)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <Loading message="백엔드 연결 확인 중..." />
  if (error) return <ErrorMessage message={error} />

  const dbOk = health.database === 'ok'

  return (
    <div className="card" style={{ fontSize: 14 }}>
      <strong>개발용 연결 상태</strong>
      <ul style={{ margin: '8px 0 0', paddingLeft: 18 }}>
        <li>
          백엔드: <span style={{ color: 'var(--color-ok)' }}>연결됨</span>{' '}
          <span className="muted">
            ({health.app} v{health.version} / {health.env})
          </span>
        </li>
        <li>
          DB:{' '}
          <span style={{ color: dbOk ? 'var(--color-ok)' : 'var(--color-error)' }}>
            {dbOk ? '연결됨' : '연결 실패'}
          </span>
          {!dbOk && (
            <span className="muted"> — db/00_create_role_and_db.sql, 01_schema.sql 실행이 필요합니다</span>
          )}
        </li>
      </ul>
    </div>
  )
}
