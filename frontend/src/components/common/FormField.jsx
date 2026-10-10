import ErrorMessage from './ErrorMessage'

// 입력칸 하나를 감싸는 틀: 라벨 + 입력칸 + 에러 문구.
//   className : 화면마다 다른 칸 모양 (예: 'register__field')
//   label     : 칸 이름
//   required  : true 면 이름 옆에 * 를 붙인다
//   error     : 에러 문구. 비어 있으면 안 보인다
//   children  : 안에 넣을 입력칸
//
// 왜 틀을 따로 뺐나 — 입력칸마다 label + input + 에러 세 줄을 적으면,
// 칸이 일곱 개인 회원가입 폼이 스무 줄 넘게 늘어난다. 그리고 한 곳에서
// 에러 줄을 빠뜨리면 그 칸만 조용히 안내가 안 뜬다.
//
// children 으로 입력칸을 받는 이유 — 칸마다 종류가 다르다
// (input · select · PasswordInput). 틀은 같고 안만 바뀐다.
//
// 쓰는 법
//   <FormField label="이메일" required error={errors.email}>
//     <input name="email" value={values.email} onChange={handleChange} />
//   </FormField>
export default function FormField({ className, label, required, error, children }) {
  return (
    // div 가 아니라 label 로 감싼다. 그러면 칸 이름을 눌러도
    // 안의 입력칸에 커서가 간다 - htmlFor 와 id 를 짝지어 적지
    // 않아도 되는 것이 label 로 감쌀 때의 이점이다
    <label className={className}>
      <span>
        {label}
        {/* 필수 표시. 안 넘기면 안 그려진다 */}
        {required && <em className="form-field__required">*</em>}
      </span>
      {/* 입력칸이 들어오는 자리 */}
      {children}
      {/* 에러가 없으면 ErrorMessage 가 스스로 아무것도 안 그린다 */}
      <ErrorMessage message={error} />
    </label>
  )
}
