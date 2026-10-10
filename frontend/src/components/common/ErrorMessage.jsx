// 에러 문구를 빨간 글씨로 보여준다. message 가 비어 있으면 아무것도 안 그린다.
//
// 왜 컴포넌트로 뺐나 — 에러를 띄우는 자리가 화면마다 있다. 각자
// 적으면 색과 여백이 조금씩 달라지고, "비어 있으면 안 그리기" 를
// 빠뜨린 곳에서는 빈 줄이 생긴다.
//
// 쓰는 법
//   <ErrorMessage message={error} />
//
// 조건부로 감싸지 않아도 된다. 비어 있으면 스스로 아무것도 안 그린다 -
// 쓰는 쪽에서 {error && <ErrorMessage .../>} 로 적지 않아도 된다.
export default function ErrorMessage({ message }) {
  // null 을 돌려주면 리액트가 아무것도 그리지 않는다.
  // 빈 <p> 를 돌려주면 글자는 없어도 여백이 생긴다
  if (!message) {
    return null
  }

  // 색을 CSS 변수로 쓴다. 'red' 로 박으면 다크 모드에서 눈이 아프다 -
  // var(--color-error) 는 테마에 따라 값이 달라진다
  return <p style={{ color: 'var(--color-error)', margin: '4px 0' }}>{message}</p>
}
