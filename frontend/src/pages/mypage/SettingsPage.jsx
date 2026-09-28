import { Link, useNavigate } from 'react-router-dom'
import Icon from '../../components/common/Icon'
import { useAiLog } from '../../contexts/AiLogContext'
import { useAuth } from '../../contexts/AuthContext'
import { useLikes } from '../../contexts/LikesContext'
import { usePrefs } from '../../contexts/PrefsContext'
import '../../styles/pages/ListPage.css'
import '../../styles/pages/SettingsPage.css'

// 설정 화면.
//
// 로그인 없이도 열린다. 여기서 바꾸는 값(테마, 알림)은 서버가 아니라
// 이 브라우저에 저장되는 거라서 로그인을 요구할 이유가 없다.
//
// 사이드바 아래에도 같은 스위치가 있는데 역할이 다르다.
// 거기는 하루에도 몇 번씩 누르는 자리고,
// 여기는 뭘 바꿀 수 있는지 한눈에 보여주고 계정까지 같이 다루는 자리다.
export default function SettingsPage() {
  const { theme, setTheme, notify, setNotify } = usePrefs()
  const { isAuthenticated, user, logout } = useAuth()
  const { count, clear } = useLikes()
  const { count: logCount, clear: clearLogs } = useAiLog()

  const navigate = useNavigate()

  // 로그아웃하고 홈으로 보낸다
  function onLogout() {
    logout()
    navigate('/')
  }

  // 찜한 레시피를 전부 지운다.
  // 되돌릴 수 없어서 먼저 물어본다.
  // window.confirm 은 브라우저가 띄우는 확인창이고 확인을 누르면 true 다.
  function clearLikes() {
    const ok = window.confirm('찜한 레시피 ' + count + '건을 모두 지울까요?')
    if (ok) {
      clear()
    }
  }

  // AI 추천 기록을 전부 지운다
  function clearAiLogs() {
    const ok = window.confirm('AI 추천 기록 ' + logCount + '건을 모두 지울까요?')
    if (ok) {
      clearLogs()
    }
  }

  // 테마 버튼 색
  let lightClass = 'chip'
  if (theme === 'light') {
    lightClass = 'chip is-active'
  }

  let darkClass = 'chip'
  if (theme === 'dark') {
    darkClass = 'chip is-active'
  }

  let notifyText = '꺼짐'
  if (notify) {
    notifyText = '켜짐'
  }

  // 찜 항목의 설명과 개수
  let likeCountText = ''
  let likeDesc = '로그인하면 찜한 레시피를 기기와 상관없이 볼 수 있어요.'
  if (isAuthenticated) {
    likeCountText = count + '건'
    likeDesc = '계정에 저장돼요. 지우면 되돌릴 수 없습니다.'
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
            <p className="settings__desc muted">
              어두운 곳에서 보실 땐 다크 모드가 눈에 편해요.
            </p>
          </div>

          <div className="settings__theme">
            <button
              type="button"
              className={lightClass}
              onClick={() => setTheme('light')}
            >
              <Icon name="sun" size={15} /> 라이트
            </button>
            <button
              type="button"
              className={darkClass}
              onClick={() => setTheme('dark')}
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
            <p className="settings__desc muted">
              내 글에 달린 댓글과 좋아요를 상단 종 아이콘에 모아서 보여드려요.
            </p>
          </div>

          {/* 체크박스를 스위치 모양으로 그린다.
              div 로 직접 만들면 키보드 조작이랑 화면낭독기 지원을
              전부 다시 붙여야 한다. */}
          <label className="switch">
            <input
              type="checkbox"
              checked={notify}
              onChange={(e) => setNotify(e.target.checked)}
            />
            <span className="switch__track" aria-hidden="true" />
            <span className="switch__text">{notifyText}</span>
          </label>
        </div>
      </section>

      {/* 내 데이터 */}
      <section className="card settings">
        <h3 className="settings__title">내 데이터</h3>

        {/* 찜은 서버에 회원별로 저장된다.
            로그인 안 했으면 보여줄 것도 지울 것도 없다. */}
        <div className="settings__row">
          <div>
            <p className="settings__label">찜한 레시피 {likeCountText}</p>
            <p className="settings__desc muted">{likeDesc}</p>
          </div>

          <div className="settings__actions">
            {isAuthenticated ? (
              <>
                <Link to="/liked" className="btn btn--ghost btn--sm">
                  목록 보기
                </Link>
                <button
                  type="button"
                  className="btn btn--ghost btn--sm settings__danger"
                  onClick={clearLikes}
                  disabled={count === 0}
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
            <p className="settings__desc muted">
              언제 어떤 조건으로 추천받았는지의 기록이에요. 이것도 이 브라우저에만
              저장됩니다.
            </p>
          </div>

          <div className="settings__actions">
            <Link to="/ai-history" className="btn btn--ghost btn--sm">
              목록 보기
            </Link>
            <button
              type="button"
              className="btn btn--ghost btn--sm settings__danger"
              onClick={clearAiLogs}
              disabled={logCount === 0}
            >
              전체 삭제
            </button>
          </div>
        </div>

        <div className="settings__row">
          <div>
            <p className="settings__label">못 먹는 재료</p>
            <p className="settings__desc muted">
              알레르기 재료는 추천에서 완전히 제외돼요.
            </p>
          </div>

          <Link to="/mypage" className="btn btn--ghost btn--sm">
            관리하기
          </Link>
        </div>
      </section>

      {/* 계정 */}
      <section className="card settings">
        <h3 className="settings__title">계정</h3>

        {isAuthenticated ? (
          <div className="settings__row">
            <div>
              <p className="settings__label">{user.nickname}님</p>
              <p className="settings__desc muted">{user.email}</p>
            </div>

            <div className="settings__actions">
              <Link to="/mypage" className="btn btn--ghost btn--sm">
                내 정보 · 탈퇴
              </Link>
              <button
                type="button"
                className="btn btn--ghost btn--sm"
                onClick={onLogout}
              >
                로그아웃
              </button>
            </div>
          </div>
        ) : (
          <div className="settings__row">
            <div>
              <p className="settings__label">로그인하지 않았어요</p>
              <p className="settings__desc muted">
                로그인하면 먹은 기록과 찜한 레시피가 쌓입니다.
              </p>
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
