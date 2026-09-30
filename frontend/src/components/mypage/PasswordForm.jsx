import { useState } from 'react'
import ErrorMessage from '../common/ErrorMessage'
import { changeMyPassword } from '../../api/users'
import { getErrorMessage } from '../../utils/apiError'

// 비밀번호 변경 폼. 마이페이지의 AccountSection 안에서 열린다.
//   onDone : 변경에 성공하면 부를 함수 (폼을 닫는다)
export default function PasswordForm({ onDone }) {
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault() // 폼을 제출하면 페이지가 새로고침되는데, 그걸 막는다

    if (newPassword.length < 8) {
      setError('새 비밀번호는 8자 이상이어야 합니다')
      return
    }
    if (newPassword !== confirmPassword) {
      setError('새 비밀번호가 서로 다릅니다')
      return
    }

    setSaving(true)
    setError('')

    try {
      await changeMyPassword(currentPassword, newPassword)
      alert('비밀번호가 변경되었습니다.')
      onDone()
    } catch (err) {
      // 현재 비밀번호가 틀리면 서버가 알려준다
      setError(getErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form className="mypage__profile" onSubmit={handleSubmit}>
      <label className="mypage__field">
        <span>현재 비밀번호</span>
        <input type="password" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} />
      </label>

      <label className="mypage__field">
        <span>새 비밀번호</span>
        <input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} />
      </label>

      <label className="mypage__field">
        <span>새 비밀번호 확인</span>
        <input type="password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} />
      </label>

      <div className="mypage__field-actions">
        <button type="submit" className="btn" disabled={saving}>
          {saving ? '변경 중…' : '비밀번호 변경'}
        </button>
        <span className="muted">8자 이상</span>
      </div>

      <ErrorMessage message={error} />
    </form>
  )
}
