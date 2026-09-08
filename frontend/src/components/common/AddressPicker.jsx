import { useEffect, useState } from 'react'
import { getRegions } from '../../api/catalog'
import '../../styles/components/common/AddressPicker.css'

/**
 * 주소 선택 — 시·도와 시·군·구를 연동된 드롭다운 두 개로 고른다.
 *
 * 회원가입과 마이페이지가 같이 쓴다. 두 곳에 따로 만들면 한쪽만 고쳤을 때
 * 가입 때 고른 주소를 수정 화면에서 못 고르는 상태가 된다.
 *
 * 왜 직접 입력이 아니라 드롭다운인가
 * ----------------------------------
 * 이 주소는 화면에 보여주기만 하는 값이 아니라 **날씨를 부르는 좌표로 바뀐다.**
 * 자유 입력이면 "서울시" / "서울특별시" / "서울" 이 뒤섞이고, 서버는 그중
 * 어느 것도 좌표로 바꾸지 못한다. 목록에서 고르게 하면 그런 일이 없다.
 *
 * 쓰는 법
 * -------
 *   <AddressPicker
 *     sido={sido}
 *     sigungu={sigungu}
 *     onChange={(nextSido, nextSigungu) => ...}
 *   />
 */

/**
 * 받아온 목록을 여기 담아둔다.
 *
 * 컴포넌트 밖(모듈 최상단)에 두는 이유: 회원가입에서 마이페이지로 옮겨갈 때
 * 같은 목록을 또 받아오지 않게 하기 위함이다. 250개가 넘는 목록이고
 * 바뀌지도 않는 값이라 한 번만 받으면 된다.
 */
let cachedRegions = null

export default function AddressPicker({ sido, sigungu, onChange, disabled }) {
  const [regions, setRegions] = useState(cachedRegions ?? [])
  const [loading, setLoading] = useState(cachedRegions === null)

  useEffect(() => {
    // 이미 받아둔 게 있으면 다시 부르지 않는다
    if (cachedRegions !== null) {
      return
    }

    let alive = true

    getRegions()
      .then((data) => {
        cachedRegions = data
        if (alive) setRegions(data)
      })
      .catch(() => {
        // 목록을 못 받아도 가입 자체는 되어야 한다.
        // 주소는 선택 항목이라 비어 있어도 다음 단계로 넘어간다.
      })
      .finally(() => {
        if (alive) setLoading(false)
      })

    return () => {
      alive = false
    }
  }, [])

  // 고른 시·도에 딸린 시·군·구 목록.
  // find 는 못 찾으면 undefined 를 준다. ?? 로 빈 배열을 받아둔다.
  const found = regions.find((item) => item.name === sido)
  const sigunguList = found?.sigungu ?? []

  /**
   * 시·도를 바꿨을 때.
   *
   * 시·군·구를 반드시 비운다. 안 비우면 "부산광역시 강남구" 처럼
   * 있지도 않은 주소가 만들어지고, 서버는 좌표를 못 찾아 날씨가 기본값으로 나간다.
   */
  function changeSido(nextSido) {
    onChange(nextSido, '')
  }

  if (loading) {
    return <p className="muted address__loading">지역 목록을 불러오는 중…</p>
  }

  if (regions.length === 0) {
    return (
      <p className="muted address__loading">
        지역 목록을 불러오지 못했습니다. 주소는 나중에 마이페이지에서 정하셔도 돼요.
      </p>
    )
  }

  return (
    <div className="address">
      <select
        className="address__select"
        value={sido ?? ''}
        onChange={(e) => changeSido(e.target.value)}
        disabled={disabled}
        aria-label="시 · 도"
      >
        <option value="">시 · 도 선택</option>
        {regions.map((item) => (
          <option key={item.name} value={item.name}>
            {item.name}
          </option>
        ))}
      </select>

      <select
        className="address__select"
        value={sigungu ?? ''}
        onChange={(e) => onChange(sido, e.target.value)}
        // 시·도를 안 골랐으면 고를 게 없다.
        // 세종특별자치시처럼 시·군·구가 없는 곳도 목록이 비어 잠긴다.
        disabled={disabled || sigunguList.length === 0}
        aria-label="시 · 군 · 구"
      >
        <option value="">
          {sigunguList.length === 0 ? '해당 없음' : '시 · 군 · 구 선택'}
        </option>
        {sigunguList.map((name) => (
          <option key={name} value={name}>
            {name}
          </option>
        ))}
      </select>
    </div>
  )
}
