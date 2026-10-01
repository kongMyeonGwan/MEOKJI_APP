import ErrorMessage from './ErrorMessage'

// 입력칸 하나를 감싸는 틀: 라벨 + 입력칸 + 에러 문구.
//   className : 화면마다 다른 칸 모양 (예: 'register__field')
//   label     : 칸 이름
//   required  : true 면 이름 옆에 * 를 붙인다
//   error     : 에러 문구. 비어 있으면 안 보인다
//   children  : 안에 넣을 입력칸
export default function FormField({ className, label, required, error, children }) {
  return (
    <label className={className}>
      <span>
        {label}
        {required && <em className="form-field__required">*</em>}
      </span>
      {children}
      <ErrorMessage message={error} />
    </label>
  )
}
