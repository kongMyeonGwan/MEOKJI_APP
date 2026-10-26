import { useEffect, useRef, useState } from 'react'

import { askChat } from '../../api/chat'
import { getBans } from '../../api/bans'

// 화면 오른쪽 아래의 재료 대체 챗봇. 로그인한 사람에게만 보인다.
// 서버가 내 못 먹는 재료를 직접 읽어서 답에서 빼 주므로, 여기서는 칩으로 보여주기만 한다.

// 서버가 받는 이전 대화 최대 개수 (ChatRequest.history 와 같게 맞춘다)
const MAX_HISTORY = 10

// 입력칸 아래 예시 질문
const EXAMPLES = ['버터 대신 뭐 쓸까?', '우유 대신 쓸 수 있는 거 있어?']

export default function ChatbotWidget({ user }) {
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState([]) // [{ role, content }]
  const [text, setText] = useState('')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')
  const [bans, setBans] = useState([])

  const bodyRef = useRef(null)

  // 창을 열 때마다 못 먹는 재료를 다시 읽는다 (마이페이지에서 바꿨을 수 있어서)
  useEffect(() => {
    if (!open) {
      return
    }

    const loadBans = async () => {
      try {
        const data = await getBans()
        setBans(data)
      } catch (err) {
        // 칩은 보여주기용이다. 못 읽어도 대화는 되므로 조용히 넘어간다
        setBans([])
      }
    }
    loadBans()
  }, [open])

  // 새 글이 생기면 맨 아래로 내린다
  useEffect(() => {
    if (bodyRef.current) {
      bodyRef.current.scrollTop = bodyRef.current.scrollHeight
    }
  }, [messages, sending, open])

  // 로그인 안 했으면 아무것도 그리지 않는다
  if (!user) {
    return null
  }

  const handleToggle = () => {
    setOpen(!open)
  }

  // 질문 보내기. question 을 넘기면 그 문장으로, 아니면 입력칸 내용으로 보낸다
  const send = async (question) => {
    const message = question.trim()

    if (!message || sending) {
      return
    }

    // 이번 질문 이전 대화만 서버에 넘긴다
    const history = messages.slice(-MAX_HISTORY)

    setError('')
    setText('')
    setSending(true)
    setMessages([...messages, { role: 'user', content: message }])

    try {
      const data = await askChat(message, history)
      setMessages((prev) => [...prev, { role: 'assistant', content: data.answer }])
    } catch (err) {
      // 실패하면 방금 질문을 되돌려서 다시 보낼 수 있게 한다
      setMessages((prev) => prev.slice(0, -1))
      setText(message)

      if (err.response) {
        // 입력값 검사에 걸리면 detail 이 배열로 온다. 예) [{ msg: '...' }]
        const detail = err.response.data.detail
        if (Array.isArray(detail)) {
          setError(detail[0].msg)
        } else {
          setError(detail || '요청을 처리하지 못했습니다.')
        }
      } else if (err.request) {
        // 백엔드를 안 켰거나 30초가 지난 경우
        setError('서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.')
      } else {
        setError(err.message)
      }
    } finally {
      setSending(false)
    }
  }

  const handleSubmit = (e) => {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다
    send(text)
  }

  const handleReset = () => {
    setMessages([])
    setError('')
  }

  return (
    <div className="chatbot">
      {open && (
        <section className="chatbot__panel" aria-label="재료 대체 챗봇">
          <header className="chatbot__head">
            <strong className="chatbot__title">🤖 재료 대체 챗봇</strong>
            <div className="chatbot__head-actions">
              {messages.length > 0 && (
                <button type="button" className="chatbot__text-btn" onClick={handleReset}>
                  새 대화
                </button>
              )}
              <button
                type="button"
                className="chatbot__close"
                onClick={handleToggle}
                aria-label="챗봇 닫기"
              >
                ✕
              </button>
            </div>
          </header>

          {/* 내가 설정한 못 먹는 재료 */}
          <div className="chatbot__bans">
            {bans.length === 0 && (
              <span className="muted chatbot__bans-empty">
                마이페이지에서 못 먹는 재료를 등록하면 답에서 빼 드려요
              </span>
            )}
            {bans.length > 0 && <span className="muted chatbot__bans-label">제외 중</span>}
            {bans.map((ban) => (
              <span
                key={ban.id}
                className={ban.banType === 'ALLERGY' ? 'chatbot__ban is-allergy' : 'chatbot__ban'}
              >
                {ban.ingredientName}
              </span>
            ))}
          </div>

          <div className="chatbot__body" ref={bodyRef}>
            <p className="chatbot__bubble is-bot">
              {user.nickname}님, 안녕하세요! 바꾸고 싶은 재료를 물어보세요. 못 먹는 재료는 빼고
              알려드릴게요.
            </p>

            {messages.length === 0 && (
              <div className="chatbot__examples">
                {EXAMPLES.map((example) => (
                  <button
                    key={example}
                    type="button"
                    className="chip"
                    onClick={() => send(example)}
                  >
                    {example}
                  </button>
                ))}
              </div>
            )}

            {messages.map((message, index) => (
              <p
                key={index}
                className={
                  message.role === 'user'
                    ? 'chatbot__bubble is-user'
                    : 'chatbot__bubble is-bot'
                }
              >
                {message.content}
              </p>
            ))}

            {sending && <p className="chatbot__bubble is-bot is-wait">AI가 찾는 중...</p>}
          </div>

          {error && <p className="chatbot__error">{error}</p>}

          <form className="chatbot__form" onSubmit={handleSubmit}>
            <input
              type="text"
              value={text}
              onChange={(e) => setText(e.target.value)}
              placeholder="예) 오이 대신 뭐 쓸까?"
              maxLength={200}
              disabled={sending}
            />
            <button type="submit" className="btn chatbot__send" disabled={sending || !text.trim()}>
              보내기
            </button>
          </form>
        </section>
      )}

      {/* 떠 있는 버튼 */}
      <button
        type="button"
        className="chatbot__fab"
        onClick={handleToggle}
        aria-label={open ? '챗봇 닫기' : '챗봇 열기'}
      >
        {open ? '✕' : '🤖'}
      </button>
    </div>
  )
}