import { useState } from 'react'
import ErrorMessage from '../../../components/common/ErrorMessage'
import PasswordInput from '../../../components/common/PasswordInput'
import useForm from '../../../hooks/useForm'
import { changeMyPassword } from '../../../api/users'

// 비밀번호 변경 폼. 마이페이지의 AccountSection 안에서 열린다.
//   onDone : 변경에 성공하면 부를 함수 (폼을 닫는다)
export default function PasswordForm({ onDone }) {
  const { values, handleChange } = useForm({ currentPassword: '', newPassword: '', confirmPassword: '' })
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault() // 폼을 제출하면 페이지가 새로고침되는데, 그걸 막는다

    if (values.newPassword.length < 8) {
      setError('새 비밀번호는 8자 이상이어야 합니다')
      return
    }
    if (values.newPassword !== values.confirmPassword) {
      setError('새 비밀번호가 서로 다릅니다')
      return
    }

    setSaving(true)
    setError('')

    try {
      await changeMyPassword(values.currentPassword, values.newPassword)
      alert('비밀번호가 변경되었습니다.')
      onDone()
    } catch (err) {
      // 현재 비밀번호가 틀리면 서버가 알려준다
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
      setSaving(false)
    }
  }

  return (
    <form className="mypage__profile" onSubmit={handleSubmit}>
      <label className="mypage__field">
        <span>현재 비밀번호</span>
        <PasswordInput
          name="currentPassword"
          autoComplete="current-password"
          value={values.currentPassword}
          onChange={handleChange}
        />
      </label>

      <label className="mypage__field">
        <span>새 비밀번호</span>
        <PasswordInput name="newPassword" autoComplete="new-password" value={values.newPassword} onChange={handleChange} />
      </label>

      <label className="mypage__field">
        <span>새 비밀번호 확인</span>
        <PasswordInput
          name="confirmPassword"
          autoComplete="new-password"
          value={values.confirmPassword}
          onChange={handleChange}
        />
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
