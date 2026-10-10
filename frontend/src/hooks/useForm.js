import { useState } from 'react'

// 입력칸이 여러 개인 폼에서 쓰는 훅.
// 입력값을 객체 하나(values)에 모아 두고, 모든 입력칸이 handleChange 하나를 같이 쓴다.
//
// 쓰는 법
//   const { values, handleChange, reset } = useForm({ email: '', password: '' })
//   <input name="email" value={values.email} onChange={handleChange} />
//
// 입력칸의 name 이 values 의 키와 같아야 한다. 다르면 그 칸만 조용히
// 입력이 안 된다 - 에러가 안 나서 알아채기 어렵다.
//
// 왜 입력칸마다 useState 를 두지 않나
//
// 회원가입 폼은 칸이 일곱 개다. 각각 useState 를 두면 선언이 일곱 줄,
// onChange 함수도 일곱 개가 된다. 객체 하나에 모으면 선언 한 줄과
// 핸들러 하나로 끝난다.
//
// 대신 치르는 것 — 한 칸만 바뀌어도 values 객체가 새로 만들어져서
// 폼 전체가 다시 그려진다. 입력칸 몇 개짜리 폼에서는 느껴지지 않는다.
//
// 쓰는 곳 — 로그인 · 회원가입 · 비밀번호 찾기 · 글쓰기 · 내 정보 수정
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

    // setValues 에 값을 바로 넣지 않고 함수를 넘긴다.
    //
    // 왜 — 값을 바로 넣으면 그때 화면이 들고 있던 values 를 쓴다.
    // 입력이 빠르게 이어지면 그 값이 한 박자 늦은 것일 수 있어서,
    // 앞의 입력이 지워진다. 함수로 넘기면 리액트가 가장 최근 값을
    // prev 로 넣어준다
    setValues((prev) => ({
      ...prev, // 나머지 값은 그대로 복사
      // 전개(...)를 안 쓰면 바뀐 칸만 남고 나머지가 사라진다
      [name]: type === 'checkbox' ? checked : value, // [name] = 변수 값을 키로 쓰는 문법
      // 체크박스만 checked 를 쓴다. value 를 읽으면 체크를 풀어도
      // 늘 "on" 이라는 글자가 들어온다
    }))
  }

  // 처음 값으로 되돌리기
  //
  // 글을 쓰고 나서 입력칸을 비울 때 쓴다. initialValues 를 그대로
  // 넣는 이유 — 빈 객체({})를 넣으면 칸 이름이 사라져서,
  // value={values.email} 이 undefined 가 되고 리액트가 경고를 낸다
  const reset = () => {
    setValues(initialValues)
  }

  // setValues 는 돌려주지 않는다. 밖에서 값을 통째로 바꿀 수 있게
  // 열어두면, 이 훅을 거치는 이유가 없어진다.
  //
  // (내 정보 수정처럼 서버에서 받은 값을 채워야 하는 화면은
  //  useForm 을 안 쓰고 useState 로 직접 다룬다)
  return { values, handleChange, reset }
}
