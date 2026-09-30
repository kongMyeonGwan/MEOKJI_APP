import { useEffect, useState } from 'react'
import { fetchHealth } from '../../api/health'
import { getErrorMessage } from '../../utils/apiError'
import Loading from './Loading'
import ErrorMessage from './ErrorMessage'

// 백엔드랑 DB 가 연결됐는지 보여주는 개발용 카드
export default function ApiStatus() {
  const [health, setHealth] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const load = async () => {
      try {
        const data = await fetchHealth()
        setHealth(data)
      } catch (err) {
        setError(getErrorMessage(err))
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  if (loading) {
    return <Loading message="백엔드 연결 확인 중..." />
  }
  if (error) {
    return <ErrorMessage message={error} />
  }

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
          {dbOk ? (
            <span style={{ color: 'var(--color-ok)' }}>연결됨</span>
          ) : (
            <span style={{ color: 'var(--color-error)' }}>
              연결 실패 — 백엔드의 DB 설정을 확인하세요
            </span>
          )}
        </li>
      </ul>
    </div>
  )
}
