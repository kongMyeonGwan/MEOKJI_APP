import { useEffect, useState } from 'react'

// 기다리는 동안 보여주는 표시 (도는 원 + 문구).
//
// steps 에 문구 배열을 넘기면 5초마다 다음 문구로 바뀐다.
// AI 가 레시피를 만드는 데 10초쯤 걸려서 화면이 멈춘 것처럼 안 보이게 하려고.
//   <Loading message="불러오는 중..." />
//   <Loading steps={['레시피 만드는 중...', '가격 찾는 중...']} />
export default function Loading({ message, steps }) {
  const [index, setIndex] = useState(0)

  useEffect(() => {
    if (!steps) {
      return
    }

    const timer = setInterval(() => {
      // 마지막 문구에서 멈춘다
      setIndex((prev) => Math.min(prev + 1, steps.length - 1))
    }, 5000)

    // 화면에서 사라질 때 타이머를 끈다
    return () => clearInterval(timer)
  }, [])

  const text = steps ? steps[index] : message || '불러오는 중...'

  return (
    <p className="loading">
      <span className="loading__spinner" />
      <span className="loading__text">{text}</span>
    </p>
  )
}
