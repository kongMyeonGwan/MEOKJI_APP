import { useState } from 'react'

// 비밀번호 입력칸 + 보기/숨기기 버튼.
//   ref      : 부모가 이 입력칸에 포커스를 줄 때 쓴다 (React 19 는 ref 를 props 로 바로 받는다)
//   ...props : name, value, onChange 같은 나머지는 그대로 <input> 에 넘긴다
//
// 왜 필요한가 — 비밀번호는 점으로 가려져서, 오타를 내도 알 수 없다.
// 회원가입에서 두 번 적게 해도 둘 다 똑같이 틀릴 수 있다.
//
// ...props 로 받아 그대로 넘기는 이유 — 이 컴포넌트가 신경 쓸 것은
// "가릴지 보일지" 뿐이다. name · value · onChange · placeholder 를
// 하나하나 받아 적으면, 새 속성을 쓸 때마다 이 파일을 고쳐야 한다.
//
// 쓰는 법 (보통 input 과 똑같이)
//   <PasswordInput name="password" value={values.password} onChange={handleChange} />
export default function PasswordInput({ ref, ...props }) {
  const [show, setShow] = useState(false) // true 면 글자가 보인다

  return (
    <span className="pw-input">
      {/* type 을 ...props 보다 뒤에 적는다. 순서가 중요하다 -
          앞에 적으면 쓰는 쪽이 type="password" 를 넘겼을 때
          그 값이 이것을 덮어써서 '보기' 가 안 먹는다 */}
      <input ref={ref} {...props} type={show ? 'text' : 'password'} />
      {/* type="button" 이 없으면 누를 때 폼이 제출된다 */}
      <button type="button" className="pw-input__toggle" onClick={() => setShow(!show)}>
        {show ? '숨기기' : '보기'}
      </button>
    </span>
  )
}
