import { useEffect, useState } from 'react'
import '../../styles/components/common/Loading.css'

/**
 * 기다리는 동안 보여주는 표시.
 *
 * 왜 글자 한 줄로는 부족한가
 * --------------------------
 * 레시피는 AI가 만들어 주는 데 7~10초가 걸린다(모델이 붐비면 30초를 넘기도 한다).
 * 그동안 회색 글자 한 줄만 떠 있으면 화면이 멈춘 것처럼 보인다.
 * 실제로 느려진 게 아니라도 사용자는 "죽었나?" 하고 새로고침을 누른다.
 *
 * 그래서 두 가지를 준다.
 *   도는 원      아직 일하고 있다는 신호
 *   바뀌는 문구  어디까지 왔는지
 *
 * steps 를 주면 문구가 순서대로 바뀐다. 서버가 진행률을 알려주지는 않으므로
 * 시간으로 넘긴다. 실제 순서(레시피 생성 -> 가격 조회)와 같은 순서로 적어야
 * 거짓말이 되지 않는다.
 */
export default function Loading({ message = '불러오는 중...', steps, stepMs = 5000 }) {
  const [index, setIndex] = useState(0)

  useEffect(() => {
    if (!steps?.length) return

    const timer = setInterval(() => {
      // 마지막 문구에서 멈춘다. 계속 돌리면 다 끝난 것처럼 보였다가
      // 처음으로 돌아가서 더 이상해진다.
      setIndex((current) => Math.min(current + 1, steps.length - 1))
    }, stepMs)

    return () => clearInterval(timer)
  }, [steps, stepMs])

  const text = steps?.length ? steps[index] : message

  return (
    <p className="loading" role="status" aria-live="polite">
      <span className="loading__spinner" aria-hidden="true" />
      <span className="loading__text">{text}</span>
    </p>
  )
}
