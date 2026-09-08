import { createContext, useContext, useEffect, useState } from 'react'
import { getNotifications, getProfile } from '../api/users'
import { useAuth } from './AuthContext'

/**
 * 요리 레벨과 알림.
 *
 * 사이드바(레벨 카드) · 상단바(종 배지) · 마이페이지가 같은 값을 봐야 한다.
 * 화면마다 따로 부르면 대시보드 하나 여는 데 같은 요청이 세 번 나가고,
 * 글을 쓴 뒤 사이드바 레벨만 안 바뀌는 일이 생긴다.
 *
 * 로그인했을 때만 불러온다. Guest 는 레벨도 알림도 없다.
 *
 * 알림의 "읽음"은 왜 브라우저가 판단하나
 * --------------------------------------
 * 서버에 알림 테이블이 없다. 내 글에 달린 댓글과 좋아요를 조회 시점에
 * 모아서 만들어 주는 것이라, 하나하나 읽었는지를 서버가 알 수 없다.
 * 대신 "마지막으로 알림을 열어본 시각"만 이 브라우저에 저장해 두고,
 * 그보다 나중에 생긴 알림을 안 읽은 것으로 센다.
 *
 * 쓰는 법
 * -------
 *   const { profile, notifications, unreadCount, markAllRead, reload } = useProfile()
 */

const READ_AT_KEY = 'meokji.notify.readAt'

const ProfileContext = createContext(null)

/** 마지막으로 알림을 확인한 시각. 없으면 빈 문자열(전부 안 읽음) */
function readSavedReadAt() {
  try {
    return localStorage.getItem(READ_AT_KEY) ?? ''
  } catch {
    // 사생활 보호 모드 등으로 localStorage 를 못 읽는 경우
    return ''
  }
}

export function ProfileProvider({ children }) {
  const { isAuthenticated } = useAuth()

  const [profile, setProfile] = useState(null)
  const [notifications, setNotifications] = useState([])
  const [readAt, setReadAt] = useState(readSavedReadAt)

  /**
   * 서버에서 다시 받아온다.
   *
   * 식사를 확정하거나 글을 쓴 직후에 부르면 레벨이 바로 올라간다.
   * 실패는 조용히 넘긴다. 레벨 카드가 안 갱신될 뿐이고, 그것 때문에
   * 방금 성공한 작업이 실패한 것처럼 보이면 안 된다.
   */
  function reload() {
    getProfile()
      .then((data) => setProfile(data))
      .catch(() => {})

    getNotifications()
      .then((data) => setNotifications(data))
      .catch(() => {})
  }

  // 로그인 상태가 바뀔 때마다 다시 맞춘다.
  // 로그아웃하면 남아 있던 남의 레벨이 화면에 보이지 않게 비운다.
  useEffect(() => {
    if (!isAuthenticated) {
      setProfile(null)
      setNotifications([])
      return
    }

    reload()
  }, [isAuthenticated])

  /** 종 아이콘을 열었을 때. 지금 시각을 "확인한 시각"으로 저장한다. */
  function markAllRead() {
    const now = new Date().toISOString()
    setReadAt(now)

    try {
      localStorage.setItem(READ_AT_KEY, now)
    } catch {
      // 저장을 못 해도 이번 화면에서는 읽은 것으로 보인다
    }
  }

  /** 이 알림이 마지막 확인 시각보다 나중에 온 것인지. */
  function isUnread(item) {
    if (!readAt) {
      return true // 한 번도 안 열어봤으면 전부 새 알림이다
    }
    return item.created_at > readAt
  }

  let unreadCount = 0
  for (const item of notifications) {
    if (isUnread(item)) {
      unreadCount += 1
    }
  }

  const value = {
    profile,
    notifications,
    unreadCount,
    isUnread,
    markAllRead,
    reload,
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
