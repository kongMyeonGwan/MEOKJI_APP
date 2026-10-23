import { useState } from 'react'

// 대체 재료 찾기 카드 (첫 화면 오른쪽, '지금 싼 재료로' 아래).
// 없는 재료를 입력하면 AI 가 대신 쓸 재료를 알려준다.
//
// TODO(LLM 담당): 지금은 UI 자리만 잡아둔 상태라 가데이터(SAMPLE_RESULTS)를 그린다.
//   1. api/ 에 대체 재료 API 함수를 만들고 handleSubmit 에서 부른다
//   2. 응답이 오기 전에는 <Loading message="AI가 찾는 중..." /> 을 보여준다
//   3. 로그인했으면 마이페이지의 못 먹는 재료(알레르기)는 후보에서 빼달라고 서버에 넘긴다
//   4. 다 되면 SAMPLE_RESULTS 와 '준비 중' 문구를 지운다

// 입력칸 아래 예시 재료. 누르면 바로 찾는다
const EXAMPLES = ['버터', '생크림', '굴소스', '청주']

// 결과 모양을 보여주려는 가데이터. 어떤 재료를 넣어도 이걸 보여준다
const SAMPLE_RESULTS = [
  { name: '올리브유', reason: '고소한 맛은 덜하지만 볶음 · 구이에 그대로 쓸 수 있어요' },
  { name: '마가린', reason: '맛과 식감이 가장 비슷해요. 같은 양으로 바꿔 쓰세요' },
  { name: '코코넛오일', reason: '굳는 성질이 비슷해 베이킹에 잘 맞아요' },
]

export default function SubstituteFinder() {
  const [keyword, setKeyword] = useState('')
  const [searched, setSearched] = useState('') // 마지막으로 찾은 재료. 비어 있으면 결과를 안 그린다

  const find = (text) => {
    if (!text.trim()) {
      return
    }
    setKeyword(text.trim())
    setSearched(text.trim())
  }

  const handleSubmit = (e) => {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다
    find(keyword)
  }

  return (
    <section className="card subfind">
      <header className="subfind__head">
        <h2 className="section-title">🔄 대체 재료 찾기</h2>
        <p className="muted subfind__sub">없는 재료를 입력하면 AI가 대신 쓸 재료를 알려줘요</p>
      </header>

      <form className="subfind__search" onSubmit={handleSubmit}>
        <input
          type="text"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="예) 버터"
          maxLength={50}
        />
        <button type="submit" className="btn subfind__submit">
          찾기
        </button>
      </form>

      <div className="subfind__examples">
        {EXAMPLES.map((text) => (
          <button key={text} type="button" className="chip" onClick={() => find(text)}>
            {text}
          </button>
        ))}
      </div>

      {searched && (
        <div className="subfind__result">
          <p className="subfind__result-title">
            <strong>{searched}</strong> 대신 이렇게 써보세요
          </p>

          <ul className="subfind__list">
            {SAMPLE_RESULTS.map((item) => (
              <li key={item.name} className="subfind__item">
                <strong className="subfind__name">{item.name}</strong>
                <span className="muted subfind__reason">{item.reason}</span>
              </li>
            ))}
          </ul>

          <p className="muted subfind__note">준비 중인 기능이라 지금은 예시 결과를 보여드려요.</p>
        </div>
      )}
    </section>
  )
}
