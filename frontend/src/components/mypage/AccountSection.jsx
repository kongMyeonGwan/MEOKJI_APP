import { useState } from 'react'
import PasswordForm from './PasswordForm'
import WithdrawForm from './WithdrawForm'

// 계정 관리 카드. 버튼을 눌러야 비밀번호 변경 / 탈퇴 폼이 열린다.
//   onWithdrawn : 탈퇴가 끝나면 부를 함수
export default function AccountSection({ onWithdrawn }) {
  // 지금 열린 폼: 'password' / 'withdraw' / null(닫힘)
  const [open, setOpen] = useState(null)

  // 열린 걸 또 누르면 닫고, 아니면 연다
  const toggle = (name) => {
    if (open === name) {
      setOpen(null)
    } else {
      setOpen(name)
    }
  }

  return (
    <section className="card">
      <h2 className="section-title">계정 관리</h2>

      <div className="mypage__account-tabs">
        <button type="button" className="btn btn--ghost btn--sm" onClick={() => toggle('password')}>
          비밀번호 변경
        </button>
        <button type="button" className="btn btn--ghost btn--sm mypage__danger" onClick={() => toggle('withdraw')}>
          회원 탈퇴
        </button>
      </div>

      {open === 'password' && <PasswordForm onDone={() => setOpen(null)} />}
      {open === 'withdraw' && <WithdrawForm onWithdrawn={onWithdrawn} />}
    </section>
  )
}
