import { useEffect, useState } from 'react'
import { useAuth } from '../contexts/AuthContext'
import ErrorMessage from '../components/common/ErrorMessage'
import ApiStatus from '../components/common/ApiStatus'
import { addBan, getBans, removeBan } from '../api/bans'
import { parseApiError } from '../utils/apiError'
import { formatDate } from '../utils/format'
import '../styles/pages/MyPage.css'

/**
 * 내 정보 + Ban 재료 관리 (STEP 9).
 *
 * Ban은 두 종류다.
 *   ALLERGY  못 먹는 것. AI 추천에서 반드시 제외한다
 *   DISLIKE  싫어하는 것. 가능하면 피하되 대체재료를 제안한다
 *
 * 이 화면은 ProtectedRoute 안에 있으므로 user는 항상 있다.
 */
export default function MyPage() {
  const { user } = useAuth()

  const [bans, setBans] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  const [name, setName] = useState('')
  const [banType, setBanType] = useState('DISLIKE')
  const [formError, setFormError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [removingId, setRemovingId] = useState(null)

  useEffect(() => {
    // 응답이 오기 전에 화면을 떠나면 setState가 헛돈다. 플래그로 막는다.
    let alive = true

    getBans()
      .then((data) => {
        if (alive) setBans(data)
      })
      .catch((err) => {
        if (alive) setLoadError(parseApiError(err).message)
      })
      .finally(() => {
        if (alive) setLoading(false)
      })

    return () => {
      alive = false
    }
  }, [])

  const onAdd = async (e) => {
    e.preventDefault()
    const trimmed = name.trim()

    if (!trimmed) {
      setFormError('재료 이름을 입력해주세요')
      return
    }
    // DB의 uq_user_bans(user_id, ingredient_name)와 같은 규칙이다.
    // 여기서 먼저 막는 건 왕복을 아끼기 위함이고, 서버도 409로 다시 막는다.
    if (bans.some((b) => b.ingredient_name === trimmed)) {
      setFormError('이미 등록한 재료입니다')
      return
    }

    setSubmitting(true)
    try {
      const created = await addBan({ ingredientName: trimmed, banType })
      // 서버가 돌려준 행을 그대로 붙인다. 목록 정렬이 등록 순이라 맨 뒤가 맞다.
      setBans((prev) => [...prev, created])
      setName('')
      setFormError('')
    } catch (err) {
      const { fieldErrors, message } = parseApiError(err)
      setFormError(fieldErrors.ingredient_name ?? message)
    } finally {
      setSubmitting(false)
    }
  }

  const onRemove = async (ban) => {
    setRemovingId(ban.id)
    try {
      await removeBan(ban.id)
      setBans((prev) => prev.filter((b) => b.id !== ban.id))
      setFormError('')
    } catch (err) {
      // 낙관적으로 먼저 지우지 않는다. 실패했는데 화면에서만 사라지면
      // 새로고침할 때 되살아나 사용자가 혼란스럽다.
      setFormError(parseApiError(err).message)
    } finally {
      setRemovingId(null)
    }
  }

  const allergies = bans.filter((b) => b.ban_type === 'ALLERGY')

  return (
    <div className="mypage">
      <section className="card">
        <h2 className="section-title">내 정보</h2>
        <dl className="mypage__info">
          <dt>닉네임</dt>
          <dd>{user?.nickname ?? '-'}</dd>
          <dt>이메일</dt>
          <dd>{user?.email ?? '-'}</dd>
          <dt>가입일</dt>
          <dd>{formatDate(user?.created_at)}</dd>
        </dl>
      </section>

      <section className="card">
        <h2 className="section-title">못 먹는 재료</h2>
        <p className="muted mypage__desc">
          여기 등록한 재료는 AI 추천과 레시피에서 제외됩니다.
          {allergies.length > 0 && (
            <>
              {' '}
              알레르기로 등록한 <strong>{allergies.length}개</strong>는 대체재료 없이
              완전히 제외합니다.
            </>
          )}
        </p>

        <form className="mypage__form" onSubmit={onAdd}>
          <input
            type="text"
            value={name}
            onChange={(e) => {
              setName(e.target.value)
              setFormError('')
            }}
            placeholder="재료 이름 (예: 오이)"
            maxLength={100}
            aria-label="재료 이름"
            disabled={submitting}
          />
          <select
            value={banType}
            onChange={(e) => setBanType(e.target.value)}
            aria-label="종류"
            disabled={submitting}
          >
            <option value="DISLIKE">싫어함</option>
            <option value="ALLERGY">알레르기</option>
          </select>
          <button type="submit" className="btn" disabled={submitting}>
            {submitting ? '추가 중…' : '추가'}
          </button>
        </form>
        <ErrorMessage message={formError} />

        {/* 목록 로딩 실패는 추가/삭제 에러와 따로 표시한다.
            둘을 한 자리에 겹쳐 쓰면 나중에 뜬 쪽이 앞의 원인을 덮는다. */}
        <ErrorMessage message={loadError} />

        {loading ? (
          <p className="empty">불러오는 중…</p>
        ) : bans.length === 0 ? (
          <p className="empty">등록한 재료가 없습니다.</p>
        ) : (
          <ul className="mypage__bans">
            {bans.map((ban) => (
              <li key={ban.id} className="mypage__ban">
                <span
                  className={`tag ${
                    ban.ban_type === 'ALLERGY' ? 'tag--allergy' : 'tag--dislike'
                  }`}
                >
                  {ban.ban_type === 'ALLERGY' ? '알레르기' : '싫어함'}
                </span>
                <span className="mypage__ban-name">{ban.ingredient_name}</span>
                <button
                  type="button"
                  className="mypage__remove"
                  onClick={() => onRemove(ban)}
                  disabled={removingId === ban.id}
                  aria-label={`${ban.ingredient_name} 삭제`}
                >
                  ✕
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* 백엔드 연동 중에 상태를 확인하는 개발용 카드.
          대시보드를 시안대로 바꾸면서 여기로 옮겼다. */}
      <ApiStatus />
    </div>
  )
}
