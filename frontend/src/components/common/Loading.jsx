import { useEffect, useState } from 'react'
import '../../styles/components/common/Loading.css'

// 기다리는 동안 보여주는 표시.
//
// 레시피는 AI 가 만드는 데 7~10초쯤 걸린다. 그동안 글자 한 줄만 있으면
// 화면이 멈춘 것처럼 보여서 사용자가 새로고침을 누른다.
// 그래서 도는 원이랑 바뀌는 문구를 같이 보여준다.
//
// steps 를 넘기면 문구가 순서대로 바뀐다.

// 문구 하나를 몇 밀리초 보여줄지
const STEP_MS = 5000

export default function Loading({ message, steps }) {
  const [index, setIndex] = useState(0)

  useEffect(() => {
    if (!steps || steps.length === 0) {
      return
    }

    const timer = setInterval(function () {
      // 마지막 문구에서 멈춘다. 계속 돌리면 처음으로 돌아가서 이상해진다.
      setIndex(function (current) {
        if (current + 1 > steps.length - 1) {
          return steps.length - 1
        }
        return current + 1
      })
    }, STEP_MS)

    return function () {
      clearInterval(timer)
    }
  }, [steps])

  let text = message
  if (!text) {
    text = '불러오는 중...'
  }
  if (steps && steps.length > 0) {
    text = steps[index]
  }

  return (
    <p className="loading">
      <span className="loading__spinner" aria-hidden="true" />
      <span className="loading__text">{text}</span>
    </p>
  )
}
