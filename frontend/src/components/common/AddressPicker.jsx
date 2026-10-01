import { useEffect, useState } from 'react'
import { getRegions } from '../../api/catalog'
import './AddressPicker.css'

// 주소 고르기. 시/도 → 시/군/구 드롭다운 두 개. 회원가입과 마이페이지에서 쓴다.
//   <AddressPicker sido={sido} sigungu={sigungu} onChange={(sido, sigungu) => ...} />
export default function AddressPicker({ sido, sigungu, onChange, disabled }) {
  // [{ name: '서울특별시', sigungu: ['강남구', ...] }, ...]
  const [regions, setRegions] = useState([])
  const [loading, setLoading] = useState(true)

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
      <select
        className="address__select"
        value={sido || ''}
        // 시/도를 바꾸면 시/군/구는 비운다 (없는 주소가 만들어지지 않게)
        onChange={(e) => onChange(e.target.value, '')}
        disabled={disabled}
      >
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
        onChange={(e) => onChange(sido, e.target.value)}
        disabled={disabled || sigunguList.length === 0}
      >
        <option value="">시 · 군 · 구 선택</option>
        {sigunguList.map((name) => (
          <option key={name} value={name}>
            {name}
          </option>
        ))}
      </select>
    </div>
  )
}
