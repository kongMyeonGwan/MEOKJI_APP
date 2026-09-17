import { useEffect, useState } from 'react'
import { getRegions } from '../../api/catalog'
import '../../styles/components/common/AddressPicker.css'

// 주소 고르기. 시/도 와 시/군/구 드롭다운 두 개가 연결되어 있다.
// 회원가입이랑 마이페이지가 같이 쓴다.
//
// 직접 입력이 아니라 드롭다운인 이유는 이 주소가 날씨를 부르는 좌표로
// 바뀌기 때문이다. "서울시" / "서울특별시" / "서울" 이 섞이면 서버가
// 좌표를 못 찾는다.
//
// 쓰는 법:
//   <AddressPicker sido={sido} sigungu={sigungu} onChange={...} />

// 받아온 목록을 여기 담아둔다.
// 컴포넌트 밖에 두면 화면을 옮겨 다녀도 다시 안 받아온다.
// 250개가 넘는데 바뀌지도 않는 목록이라 한 번만 받으면 된다.
let cachedRegions = null

export default function AddressPicker({ sido, sigungu, onChange, disabled }) {
  let firstRegions = []
  if (cachedRegions !== null) {
    firstRegions = cachedRegions
  }

  const [regions, setRegions] = useState(firstRegions)
  const [loading, setLoading] = useState(cachedRegions === null)

  useEffect(() => {
    // 이미 받아둔 게 있으면 다시 안 부른다
    if (cachedRegions !== null) {
      return
    }

    let alive = true

    getRegions()
      .then(function (data) {
        cachedRegions = data
        if (alive) {
          setRegions(data)
        }
      })
      .catch(function () {
        // 목록을 못 받아와도 가입은 돼야 한다. 주소는 선택 항목이다.
      })
      .finally(function () {
        if (alive) {
          setLoading(false)
        }
      })

    return function () {
      alive = false
    }
  }, [])

  // 고른 시/도 에 딸린 시/군/구 목록을 찾는다
  let sigunguList = []
  for (let i = 0; i < regions.length; i++) {
    if (regions[i].name === sido) {
      sigunguList = regions[i].sigungu
      break
    }
  }

  // 시/도를 바꾸면 시/군/구는 반드시 비운다.
  // 안 비우면 "부산광역시 강남구" 같은 없는 주소가 만들어진다.
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

  let sidoValue = sido
  if (!sidoValue) {
    sidoValue = ''
  }

  let sigunguValue = sigungu
  if (!sigunguValue) {
    sigunguValue = ''
  }

  let sigunguPlaceholder = '시 · 군 · 구 선택'
  if (sigunguList.length === 0) {
    sigunguPlaceholder = '해당 없음'
  }

  return (
    <div className="address">
      <select
        className="address__select"
        value={sidoValue}
        onChange={(e) => changeSido(e.target.value)}
        disabled={disabled}
        aria-label="시 · 도"
      >
        <option value="">시 · 도 선택</option>
        {regions.map(function (item) {
          return (
            <option key={item.name} value={item.name}>
              {item.name}
            </option>
          )
        })}
      </select>

      <select
        className="address__select"
        value={sigunguValue}
        onChange={(e) => onChange(sido, e.target.value)}
        // 시/도를 안 골랐으면 고를 게 없다.
        // 세종특별자치시처럼 시/군/구가 없는 곳도 목록이 비어서 잠긴다.
        disabled={disabled || sigunguList.length === 0}
        aria-label="시 · 군 · 구"
      >
        <option value="">{sigunguPlaceholder}</option>
        {sigunguList.map(function (name) {
          return (
            <option key={name} value={name}>
              {name}
            </option>
          )
        })}
      </select>
    </div>
  )
}
