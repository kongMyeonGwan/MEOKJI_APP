import { useState } from 'react'
import ErrorMessage from '../common/ErrorMessage'
import { withdraw } from '../../api/users'
import { getErrorMessage } from '../../utils/apiError'

// 회원 탈퇴 폼. 마이페이지의 AccountSection 안에서 열린다.
//   onWithdrawn : 탈퇴에 성공하면 부를 함수 (로그아웃하고 홈으로 보낸다)
export default function WithdrawForm({ onWithdrawn }) {
  const [password, setPassword] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault() // 폼을 제출하면 페이지가 새로고침되는데, 그걸 막는다

    // 되돌릴 수 없으니까 한 번 더 물어본다 (확인을 누르면 true)
    const ok = window.confirm('정말 탈퇴하시겠어요?\n작성하신 글과 기록이 모두 보이지 않게 됩니다.')
    if (!ok) {
      return
    }

    setSaving(true)
    setError('')

    try {
      await withdraw(password)
      alert('탈퇴 처리되었습니다. 그동안 이용해주셔서 감사합니다.')
      onWithdrawn()
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form className="mypage__profile" onSubmit={handleSubmit}>
      <p className="muted mypage__desc">
        탈퇴하면 작성하신 글 · 댓글 · 식사 기록이 모두 보이지 않게 됩니다. 같은 이메일로 다시 가입할 수는 있지만, 예전
        기록은 돌아오지 않습니다.
      </p>

      <label className="mypage__field">
        <span>비밀번호 확인</span>
        <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
      </label>

      <div className="mypage__field-actions">
        <button type="submit" className="btn btn--outline mypage__danger" disabled={saving}>
          {saving ? '처리 중…' : '탈퇴하기'}
        </button>
      </div>

      <ErrorMessage message={error} />
    </form>
  )
}
