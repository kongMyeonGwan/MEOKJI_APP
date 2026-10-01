import { useState } from 'react'
import './PasswordInput.css'

// 비밀번호 입력칸 + 보기/숨기기 버튼.
//   ref      : 부모가 이 입력칸에 포커스를 줄 때 쓴다 (React 19 는 ref 를 props 로 바로 받는다)
//   ...props : name, value, onChange 같은 나머지는 그대로 <input> 에 넘긴다
export default function PasswordInput({ ref, ...props }) {
  const [show, setShow] = useState(false) // true 면 글자가 보인다

  return (
    <span className="pw-input">
      <input ref={ref} {...props} type={show ? 'text' : 'password'} />
      {/* type="button" 이 없으면 누를 때 폼이 제출된다 */}
      <button type="button" className="pw-input__toggle" onClick={() => setShow(!show)}>
        {show ? '숨기기' : '보기'}
      </button>
    </span>
  )
}
