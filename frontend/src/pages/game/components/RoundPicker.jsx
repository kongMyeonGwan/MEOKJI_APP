import { useState } from 'react'
import { Link } from 'react-router-dom'

// 게임 시작 전에 크기(라운드)를 고르는 화면. 월드컵(64 · 32 · 16강)과 밸런스(16 · 8 · 4강)가 같이 쓴다.
//
//   위   : 아이콘 + 게임 이름 + 안내 태그
//   가운데: 라운드 드롭다운 + 안내 문장 + 진행 순서
//   아래 : [시작하기] [게임 목록]
//
// props
//   icon         : 맨 위 큰 이모지
//   title        : 게임 이름
//   tags         : 제목 아래 작은 안내들 ['요리 100종', ...]
//   label        : 드롭다운 위 글자
//   choices      : [{ value: 64, label: '64강' }, ...]
//   defaultValue : 처음에 골라져 있을 값
//   describe     : (value) => 드롭다운 아래 안내 문장
//   steps        : (value) => 진행 순서 ['32강', '16강', ..., '결승']
//   onStart      : 시작하기를 누르면 고른 value 를 넘겨 부른다
//
// 스타일은 styles/index.css 의 '메뉴 게임' 구역(RoundPicker)에 있다.
export default function RoundPicker({ icon, title, tags, label, choices, defaultValue, describe, steps, onStart }) {
  const [value, setValue] = useState(defaultValue)

  // select 의 값은 늘 글자로 온다. 숫자로 바꿔서 들고 있는다
  const handleChange = (e) => {
    setValue(Number(e.target.value))
  }

  const handleStart = () => {
    onStart(value)
  }

  return (
    // 다른 목록 화면(.list-page)은 왼쪽에 붙지만, 이 화면은 시작 안내 창이라 가운데에 둔다
    <div className="round-pick-page">
      <section className="card round-pick">
        {/* ----- 위: 아이콘 · 제목 · 태그 ----- */}
        <header className="round-pick__head">
          <span className="round-pick__icon">{icon}</span>
          <h2 className="round-pick__title">{title}</h2>

          <ul className="round-pick__tags">
            {tags.map((tag) => (
              <li key={tag}>{tag}</li>
            ))}
          </ul>
        </header>

        {/* ----- 가운데: 라운드 고르기 ----- */}
        <div className="round-pick__body">
          <label className="round-pick__label" htmlFor="round-pick-select">
            {label}
          </label>

          <select id="round-pick-select" className="round-pick__select" value={value} onChange={handleChange}>
            {choices.map((choice) => (
              <option key={choice.value} value={choice.value}>
                {choice.label}
              </option>
            ))}
          </select>

          <p className="round-pick__desc">{describe(value)}</p>

          {/* 진행 순서. 마지막 칸만 강조색으로 칠한다 */}
          <ol className="round-pick__steps">
            {steps(value).map((step, index, list) => (
              <li
                key={step}
                className={index === list.length - 1 ? 'round-pick__step round-pick__step--last' : 'round-pick__step'}
              >
                {step}
              </li>
            ))}
          </ol>
        </div>

        {/* ----- 아래: 버튼 ----- */}
        <footer className="round-pick__foot">
          <button type="button" className="btn round-pick__start" onClick={handleStart}>
            시작하기
          </button>
          <Link to="/game" className="btn btn--ghost round-pick__back">
            게임 목록
          </Link>
        </footer>
      </section>
    </div>
  )
}
