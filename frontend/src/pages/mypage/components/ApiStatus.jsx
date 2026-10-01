import { useEffect, useState } from 'react'
import { fetchHealth } from '../../../api/health'
import Loading from '../../../components/common/Loading'
import ErrorMessage from '../../../components/common/ErrorMessage'

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
