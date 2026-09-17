import { createContext, useContext, useEffect, useState } from 'react'
import { getNotifications, getProfile } from '../api/users'
import { useAuth } from './AuthContext'

// 내 정보랑 알림을 앱 전체에서 같이 쓰기 위한 Context.
// 상단바(종 배지)랑 마이페이지가 같은 값을 봐야 하는데 화면마다 따로 부르면
// 같은 요청이 여러 번 나간다.
//
// 로그인했을 때만 불러온다. Guest 는 알림이 없다.
//
// 알림의 "읽음" 은 브라우저가 판단한다. 서버에 알림 테이블이 없어서
// 하나하나 읽었는지를 서버가 알 수 없기 때문이다.
// 대신 마지막으로 알림을 열어본 시각만 저장해두고 그보다 나중에 생긴 것을
// 안 읽은 것으로 센다.
//
// 쓰는 법:
//   const { profile, notifications, unreadCount, markAllRead, reload } = useProfile()

const READ_AT_KEY = 'meokji.notify.readAt'

const ProfileContext = createContext(null)

// 마지막으로 알림을 확인한 시각. 없으면 빈 문자열(전부 안 읽음)
function readSavedReadAt() {
  try {
    const saved = localStorage.getItem(READ_AT_KEY)
    if (!saved) {
      return ''
    }
    return saved
  } catch (err) {
    // 시크릿 모드 같은 이유로 localStorage 를 못 읽는 경우
    return ''
  }
}

export function ProfileProvider({ children }) {
  const { isAuthenticated } = useAuth()

  const [profile, setProfile] = useState(null)
  const [notifications, setNotifications] = useState([])
  const [readAt, setReadAt] = useState(readSavedReadAt)

  // 서버에서 다시 받아온다.
  // 식사를 확정하거나 글을 쓴 직후에 부르면 숫자가 바로 바뀐다.
  // 실패는 그냥 넘긴다. 이것 때문에 방금 성공한 작업이 실패한 것처럼
  // 보이면 안 되기 때문이다.
  function reload() {
    getProfile()
      .then(function (data) {
        setProfile(data)
      })
      .catch(function () {})

    getNotifications()
      .then(function (data) {
        setNotifications(data)
      })
      .catch(function () {})
  }

  // 로그인 상태가 바뀌면 다시 맞춘다.
  // 로그아웃했을 때 남의 정보가 화면에 남지 않게 비운다.
  useEffect(() => {
    if (!isAuthenticated) {
      setProfile(null)
      setNotifications([])
      return
    }

    reload()
  }, [isAuthenticated])

  // 종 아이콘을 열었을 때. 지금 시각을 확인한 시각으로 저장한다.
  function markAllRead() {
    const now = new Date().toISOString()
    setReadAt(now)

    try {
      localStorage.setItem(READ_AT_KEY, now)
    } catch (err) {
      // 저장을 못 해도 이번 화면에서는 읽은 걸로 보인다
    }
  }

  // 이 알림이 마지막으로 확인한 시각보다 나중에 온 것인지
  function isUnread(item) {
    if (!readAt) {
      // 한 번도 안 열어봤으면 전부 새 알림이다
      return true
    }
    return item.created_at > readAt
  }

  // 안 읽은 알림 개수 세기
  let unreadCount = 0
  for (let i = 0; i < notifications.length; i++) {
    if (isUnread(notifications[i])) {
      unreadCount = unreadCount + 1
    }
  }

  const value = {
    profile: profile,
    notifications: notifications,
    unreadCount: unreadCount,
    isUnread: isUnread,
    markAllRead: markAllRead,
    reload: reload,
  }

  return (
    <ProfileContext.Provider value={value}>{children}</ProfileContext.Provider>
  )
}

export function useProfile() {
  const context = useContext(ProfileContext)

  if (context === null) {
    throw new Error('useProfile()는 <ProfileProvider> 안에서만 쓸 수 있습니다.')
  }

  return context
}
