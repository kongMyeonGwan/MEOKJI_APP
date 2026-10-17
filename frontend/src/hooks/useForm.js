import { useState } from 'react'

// 입력칸이 여러 개인 폼에서 쓰는 훅.
// 입력값을 객체 하나(values)에 모아 두고, 모든 입력칸이 handleChange 하나를 같이 쓴다.
//
// 쓰는 법
//   const { values, handleChange, reset } = useForm({ email: '', password: '' })
//   <input name="email" value={values.email} onChange={handleChange} />
//
// 입력칸의 name 이 values 의 키와 같아야 한다.
export default function useForm(initialValues) {
  const [values, setValues] = useState(initialValues)

  // 모든 입력칸이 같이 쓰는 change 핸들러
  const handleChange = (e) => {
    // e.target = 방금 바뀐 입력칸. 필요한 것만 꺼낸다
    //   name    : 입력칸의 name → values 의 어느 칸을 바꿀지 알려주는 "주소"
    //   value   : 입력된 값 (항상 문자열)
    //   type    : 'text', 'checkbox' ...
    //   checked : 체크박스가 체크됐는지 (true/false)
    const { name, value, type, checked } = e.target

    setValues((prev) => ({
      ...prev, // 나머지 값은 그대로 복사
      [name]: type === 'checkbox' ? checked : value, // [name] = 변수 값을 키로 쓰는 문법
    }))
  }

  // 처음 값으로 되돌리기
  const reset = () => {
    setValues(initialValues)
  }

  return { values, handleChange, reset }
}
