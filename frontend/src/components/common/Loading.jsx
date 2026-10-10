import { useEffect, useState } from 'react'

// 기다리는 동안 보여주는 표시 (도는 원 + 문구).
//
// steps 에 문구 배열을 넘기면 5초마다 다음 문구로 바뀐다.
// AI 가 레시피를 만드는 데 10초쯤 걸려서 화면이 멈춘 것처럼 안 보이게 하려고.
//   <Loading message="불러오는 중..." />
//   <Loading steps={['레시피 만드는 중...', '가격 찾는 중...']} />
//
// 왜 문구가 바뀌어야 하나 — 같은 문구가 10초 넘게 그대로면 멈춘 줄 안다.
// 사용자가 새로고침하면 서버를 처음부터 다시 부르게 된다.
//
// 둘 중 하나만 넘긴다. 둘 다 넘기면 steps 가 이긴다.
export default function Loading({ message, steps }) {
  const [index, setIndex] = useState(0)

  useEffect(() => {
    // 문구가 하나(message)면 바꿀 것이 없다. 타이머를 만들지 않는다
    if (!steps) {
      return
    }

    const timer = setInterval(() => {
      // 마지막 문구에서 멈춘다
      //
      // Math.min 이 없으면 index 가 배열 길이를 넘어 undefined 가
      // 되고, 화면에서 문구가 통째로 사라진다
      setIndex((prev) => Math.min(prev + 1, steps.length - 1))
    }, 5000)

    // 화면에서 사라질 때 타이머를 끈다
    //
    // 안 끄면 로딩이 끝난 뒤에도 5초마다 setIndex 가 불린다.
    // 이미 사라진 컴포넌트의 상태를 바꾸려 하는 것이라 경고가 뜨고,
    // 화면을 여러 번 드나들면 타이머가 그만큼 쌓인다
    return () => clearInterval(timer)
    // 의존성 배열이 비어 있다 - 처음 한 번만 타이머를 만든다.
    //
    // steps 를 넣어야 맞지만, 쓰는 쪽에서 배열을 그 자리에 적기 때문에
    // (steps={['...', '...']}) 다시 그려질 때마다 새 배열이 되어
    // 타이머가 매번 다시 만들어진다. 그러면 문구가 안 넘어간다.
    // 지금은 steps 가 뜨는 동안 바뀌지 않으므로 비워 두었다
  }, [])

  // 셋 중 하나를 고른다 — steps 의 지금 문구 · message · 기본 문구.
  //
  // 기본 문구를 두는 이유 — 둘 다 안 넘기고 <Loading /> 만 적는
  // 자리가 있다. 빈 글자면 도는 원만 덩그러니 보인다
  const text = steps ? steps[index] : message || '불러오는 중...'

  return (
    <p className="loading">
      <span className="loading__spinner" />
      <span className="loading__text">{text}</span>
    </p>
  )
}
