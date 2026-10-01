import { useEffect, useImperativeHandle, useRef } from 'react'
import '../../styles/components/auth/CodeInput.css'

// 인증번호 칸 (숫자 한 칸에 하나). 숫자를 치면 다음 칸으로 자동으로 넘어간다.
//   digits   : 칸마다 들어 있는 숫자 배열. 예) ['1', '2', '', '', '', '']
//   onChange : 숫자가 바뀌면 새 배열을 넘겨준다
//   ref      : 부모가 ref.current.focus(번호) 로 원하는 칸에 커서를 넣을 수 있다
export default function CodeInput({ ref, digits, onChange }) {
  const inputsRef = useRef([]) // 칸(input)들을 담을 배열 상자

  // 부모에게 쓸 수 있는 기능을 공개한다
  useImperativeHandle(ref, () => ({
    focus: (index) => inputsRef.current[index].focus(),
  }))

  // 화면이 열리면 첫 칸에 커서
  useEffect(() => {
    inputsRef.current[0].focus()
  }, [])

  // 숫자를 입력하면 저장하고 다음 칸으로 이동
  const handleChange = (index, e) => {
    const value = e.target.value.replace(/\D/g, '').slice(-1) // 숫자만, 마지막 한 글자만
    const next = [...digits] // 배열 복사 후 수정
    next[index] = value
    onChange(next)

    if (value && index < digits.length - 1) {
      inputsRef.current[index + 1].focus() // 다음 칸으로
    }
  }

  // 빈 칸에서 Backspace → 이전 칸으로 이동
  const handleKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !digits[index] && index > 0) {
      inputsRef.current[index - 1].focus()
    }
  }

  // 메일에서 복사한 번호를 붙여넣으면 앞 칸부터 한 글자씩 채운다
  const handlePaste = (e) => {
    e.preventDefault()
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, digits.length)
    const next = digits.map((d, i) => pasted[i] || '')
    onChange(next)

    const lastIndex = Math.min(pasted.length, digits.length - 1)
    inputsRef.current[lastIndex].focus()
  }

  return (
    <div className="code-input">
      {digits.map((d, i) => (
        <input
          key={i} // 칸 순서가 바뀌지 않으니 index 를 key 로 써도 된다
          ref={(el) => {
            inputsRef.current[i] = el // i 번째 칸 저장
          }}
          className="code-input__box"
          value={d}
          inputMode="numeric" // 휴대폰에서 숫자 키패드
          aria-label={`${i + 1}번째 숫자`} // 라벨이 없는 칸을 위한 이름
          onChange={(e) => handleChange(i, e)}
          onKeyDown={(e) => handleKeyDown(i, e)}
          onPaste={handlePaste}
        />
      ))}
    </div>
  )
}
