import { useEffect, useState } from 'react'
import { getRegions, getDongs, getNearestRegion } from '../../api/catalog'

// 주소 고르기. 시/도 → 시/군/구 → 읍/면/동 드롭다운 세 개.
// 회원가입과 마이페이지에서 쓴다.
//   <AddressPicker sido={sido} sigungu={sigungu} dong={dong} onChange={(sido, sigungu, dong) => ...} />
//
// 시/도 목록 맨 위에 '현재 위치로 채우기'가 있다. 고르면 브라우저에 위치를 물어보고
// 가장 가까운 동을 서버에서 받아 세 칸을 한 번에 채운다.
//
// 동 목록은 시/군/구를 고른 뒤에 따로 받아온다. 전국 동이 3,500개라 처음부터
// 다 들고 있으면 첫 화면이 400KB 를 더 내려받게 된다.

// 시/도 드롭다운에서 '현재 위치로 채우기'를 나타내는 값. 진짜 시/도 이름과 겹치면 안 된다.
const GPS_VALUE = '__gps__'

export default function AddressPicker({ sido, sigungu, dong, onChange, disabled }) {
  // [{ name: '서울특별시', sigungu: ['강남구', ...] }, ...]
  const [regions, setRegions] = useState([])
  const [loading, setLoading] = useState(true)
  const [dongs, setDongs] = useState([]) // 고른 시/군/구의 동 목록
  const [locating, setLocating] = useState(false) // 내 위치를 찾는 중인지
  const [locateError, setLocateError] = useState('')

  useEffect(() => {
    const load = async () => {
      try {
        const data = await getRegions()
        setRegions(data)
      } catch (err) {
        // 주소는 선택 항목이라 못 받아와도 넘어간다
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  // 시/군/구가 바뀔 때마다 그 아래 동 목록을 받아온다
  useEffect(() => {
    const load = async () => {
      if (!sido || !sigungu) {
        setDongs([])
        return
      }
      try {
        const data = await getDongs(sido, sigungu)
        setDongs(data)
      } catch (err) {
        setDongs([])
      }
    }
    load()
  }, [sido, sigungu])

  // 좌표를 받았으면 서버에 가장 가까운 동을 물어보고 세 칸을 채운다
  const fillByCoords = async (lat, lon) => {
    try {
      const found = await getNearestRegion(lat, lon)
      onChange(found.sido, found.sigungu, found.dong)
    } catch (err) {
      setLocateError('가까운 지역을 찾지 못했습니다. 직접 골라주세요.')
    } finally {
      setLocating(false)
    }
  }

  // '현재 위치로 채우기'를 골랐을 때. 브라우저가 위치를 알려주면 fillByCoords 로 넘긴다
  const findMyLocation = () => {
    if (!navigator.geolocation) {
      setLocateError('이 브라우저는 위치 찾기를 지원하지 않습니다.')
      return
    }

    setLocating(true)
    setLocateError('')

    navigator.geolocation.getCurrentPosition(
      // 위치를 받았을 때
      (position) => {
        fillByCoords(position.coords.latitude, position.coords.longitude)
      },
      // 위치 권한을 거부했거나 실패했을 때
      () => {
        setLocating(false)
        setLocateError('위치를 가져오지 못했습니다. 직접 골라주세요.')
      },
      { timeout: 5000 },
    )
  }

  // 시/도를 바꿨을 때. 맨 위 항목이면 위치를 찾고, 아니면 아래 두 칸을 비운다
  const changeSido = (value) => {
    if (value === GPS_VALUE) {
      findMyLocation()
      return
    }
    // 위를 바꾸면 아래는 비운다 (없는 주소가 만들어지지 않게)
    onChange(value, '', '')
  }

  if (loading) {
    return <p className="muted address__loading">지역 목록을 불러오는 중…</p>
  }

  if (regions.length === 0) {
    return <p className="muted address__loading">지역 목록을 불러오지 못했습니다.</p>
  }

  // 고른 시/도에 딸린 시/군/구 목록
  const selected = regions.find((region) => region.name === sido)
  const sigunguList = selected ? selected.sigungu : []

  return (
    <div className="address">
      <div className="address__row">
        <select
          className="address__select"
          value={sido || ''}
          onChange={(e) => changeSido(e.target.value)}
          disabled={disabled || locating}
        >
          <option value={GPS_VALUE}>📍 현재 위치로 채우기</option>
          <option value="">시 · 도 선택</option>
          {regions.map((region) => (
            <option key={region.name} value={region.name}>
              {region.name}
            </option>
          ))}
        </select>

        <select
          className="address__select"
          value={sigungu || ''}
          onChange={(e) => onChange(sido, e.target.value, '')}
          disabled={disabled || locating || sigunguList.length === 0}
        >
          <option value="">시 · 군 · 구 선택</option>
          {sigunguList.map((name) => (
            <option key={name} value={name}>
              {name}
            </option>
          ))}
        </select>

        <select
          className="address__select"
          value={dong || ''}
          onChange={(e) => onChange(sido, sigungu, e.target.value)}
          disabled={disabled || locating || dongs.length === 0}
        >
          <option value="">읍 · 면 · 동 선택</option>
          {dongs.map((name) => (
            <option key={name} value={name}>
              {name}
            </option>
          ))}
        </select>
      </div>

      {locating && <p className="muted address__hint">내 위치를 찾는 중…</p>}
      {locateError && <p className="address__hint address__hint--error">{locateError}</p>}
      {!locating && !locateError && sido && (
        <p className="muted address__hint">실제 위치와 다르면 직접 골라주세요.</p>
      )}
    </div>
  )
}
