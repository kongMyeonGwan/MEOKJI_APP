import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Icon from '../../components/common/Icon'
import { clearLikes, getLikes } from '../../api/likes'
import { clearLogs, getLogs } from '../../utils/aiLog'

// 설정 화면. 로그인 없이도 열린다 (테마, 알림은 브라우저에 저장되는 값이라).
// user, theme, notify 와 바꾸는 함수들은 App.jsx 가 props 로 넘겨준다.
export default function SettingsPage({ user, onLogout, theme, onThemeChange, notify, onNotifyChange }) {
  const [likeCount, setLikeCount] = useState(0) // 찜 개수 (서버)
  const [logCount, setLogCount] = useState(getLogs().length) // AI 추천 기록 개수 (브라우저)

  const navigate = useNavigate()

  // 로그인했으면 찜 개수를 받아온다
  useEffect(() => {
    if (!user) {
      return
    }

    const load = async () => {
      try {
        const data = await getLikes()
        setLikeCount(data.count)
      } catch (err) {
        // 개수가 0 으로 보일 뿐이다
      }
    }
    load()
  }, [user])

  const handleLogout = () => {
    onLogout()
    navigate('/')
  }

  // 찜 전체 삭제
  const handleClearLikes = async () => {
    if (!window.confirm(`찜한 레시피 ${likeCount}건을 모두 지울까요?`)) {
      return
    }
    try {
      await clearLikes()
      setLikeCount(0)
    } catch (err) {
      alert('지우지 못했습니다.')
    }
  }

  // AI 추천 기록 전체 삭제
  const handleClearLogs = () => {
    if (window.confirm(`AI 추천 기록 ${logCount}건을 모두 지울까요?`)) {
      clearLogs()
      setLogCount(0)
    }
  }

  return (
    <div className="list-page list-page--narrow">
      <div className="section-head">
        <h2 className="section-title">설정</h2>
      </div>

      {/* 화면 */}
      <section className="card settings">
        <h3 className="settings__title">화면</h3>
        <div className="settings__row">
          <div>
            <p className="settings__label">테마</p>
            <p className="settings__desc muted">어두운 곳에서 보실 땐 다크 모드가 눈에 편해요.</p>
          </div>
          <div className="settings__theme">
            <button
              type="button"
              className={theme === 'light' ? 'chip is-active' : 'chip'}
              onClick={() => onThemeChange('light')}
            >
              <Icon name="sun" size={15} /> 라이트
            </button>
            <button
              type="button"
              className={theme === 'dark' ? 'chip is-active' : 'chip'}
              onClick={() => onThemeChange('dark')}
            >
              <Icon name="moon" size={15} /> 다크
            </button>
          </div>
        </div>
      </section>

      {/* 알림 */}
      <section className="card settings">
        <h3 className="settings__title">알림</h3>
        <div className="settings__row">
          <div>
            <p className="settings__label">댓글 · 좋아요 알림</p>
            <p className="settings__desc muted">내 글에 달린 댓글과 좋아요를 상단 종 아이콘에 모아서 보여드려요.</p>
          </div>
          {/* 체크박스를 CSS 로 스위치 모양으로 꾸몄다 */}
          <label className="switch">
            <input type="checkbox" checked={notify} onChange={(e) => onNotifyChange(e.target.checked)} />
            <span className="switch__track" />
            <span className="switch__text">{notify ? '켜짐' : '꺼짐'}</span>
          </label>
        </div>
      </section>

      {/* 내 데이터 */}
      <section className="card settings">
        <h3 className="settings__title">내 데이터</h3>

        <div className="settings__row">
          <div>
            <p className="settings__label">찜한 레시피 {user && `${likeCount}건`}</p>
            <p className="settings__desc muted">
              {user ? '계정에 저장돼요. 지우면 되돌릴 수 없습니다.' : '로그인하면 찜한 레시피를 볼 수 있어요.'}
            </p>
          </div>
          <div className="settings__actions">
            {user ? (
              <>
                <Link to="/liked" className="btn btn--ghost btn--sm">
                  목록 보기
                </Link>
                <button
                  type="button"
                  className="btn btn--ghost btn--sm settings__danger"
                  onClick={handleClearLikes}
                  disabled={likeCount === 0}
                >
                  전체 삭제
                </button>
              </>
            ) : (
              <Link to="/login" className="btn btn--ghost btn--sm">
                로그인
              </Link>
            )}
          </div>
        </div>

        <div className="settings__row">
          <div>
            <p className="settings__label">AI 추천 기록 {logCount}건</p>
            <p className="settings__desc muted">언제 어떤 조건으로 추천받았는지의 기록이에요. 이 브라우저에만 저장됩니다.</p>
          </div>
          <div className="settings__actions">
            <Link to="/ai-history" className="btn btn--ghost btn--sm">
              목록 보기
            </Link>
            <button
              type="button"
              className="btn btn--ghost btn--sm settings__danger"
              onClick={handleClearLogs}
              disabled={logCount === 0}
            >
              전체 삭제
            </button>
          </div>
        </div>

        <div className="settings__row">
          <div>
            <p className="settings__label">못 먹는 재료</p>
            <p className="settings__desc muted">알레르기 재료는 추천에서 완전히 제외돼요.</p>
          </div>
          <Link to="/mypage" className="btn btn--ghost btn--sm">
            관리하기
          </Link>
        </div>
      </section>

      {/* 계정 */}
      <section className="card settings">
        <h3 className="settings__title">계정</h3>
        {user ? (
          <div className="settings__row">
            <div>
              <p className="settings__label">{user.nickname}님</p>
              <p className="settings__desc muted">{user.email}</p>
            </div>
            <div className="settings__actions">
              <Link to="/mypage" className="btn btn--ghost btn--sm">
                내 정보 · 탈퇴
              </Link>
              <button type="button" className="btn btn--ghost btn--sm" onClick={handleLogout}>
                로그아웃
              </button>
            </div>
          </div>
        ) : (
          <div className="settings__row">
            <div>
              <p className="settings__label">로그인하지 않았어요</p>
              <p className="settings__desc muted">로그인하면 먹은 기록과 찜한 레시피가 쌓입니다.</p>
            </div>
            <Link to="/login" className="btn btn--sm">
              로그인
            </Link>
          </div>
        )}
      </section>

      <p className="muted settings__version">오늘 뭐 먹지? · 개발 중 버전</p>
    </div>
  )
}
