import { useEffect, useState } from 'react'
import { getRegions } from '../../api/catalog'

// 주소 고르기. 시/도 → 시/군/구 드롭다운 두 개. 회원가입과 마이페이지에서 쓴다.
//   <AddressPicker sido={sido} sigungu={sigungu} onChange={(sido, sigungu) => ...} />
//
// 왜 주소를 받나 — 서버가 그것으로 좌표를 찾아 저장하고, 그 좌표로
// 날씨를 불러 추천에 쓴다. 주소를 글자로 치게 하면 좌표를 찾을 수 없다.
//
// 값을 들고 있지 않다. 고른 값(sido · sigungu)을 부모에게서 받고,
// 바뀌면 onChange 로 알려주기만 한다 - 그래야 부모가 그 값을
// 서버로 보낼 수 있다.
//
// onChange 가 둘을 같이 넘기는 이유 — 시/도를 바꾸면 시/군/구도
// 같이 비워야 한다. 따로 알리면 부모가 그 규칙을 또 적어야 한다.
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
        //
        // 회원가입을 막지 않는다. 주소 없이 가입하면 날씨만
        // 기본 지역으로 보여주면 된다
      } finally {
        // finally 라서 성공해도 실패해도 불린다. catch 에만 적으면
        // 성공했을 때 "불러오는 중" 이 안 사라진다
        setLoading(false)
      }
    }
    load()
    // 처음 한 번만. 행정구역은 서버가 파일에서 읽어 주는 고정값이다
  }, [])

  if (loading) {
    return <p className="muted address__loading">지역 목록을 불러오는 중…</p>
  }

  if (regions.length === 0) {
    return <p className="muted address__loading">지역 목록을 불러오지 못했습니다.</p>
  }

  // 고른 시/도에 딸린 시/군/구 목록
  //
  // 서버가 한 번에 다 보내준다(시·도 안에 시·군·구 배열이 들어 있다).
  // 시/도를 고를 때마다 서버를 다시 부르지 않아도 된다 - 25KB 를
  // 한 번 받고 그 안에서 찾는 쪽이 빠르다
  const selected = regions.find((region) => region.name === sido)
  // 아직 시/도를 안 골랐으면 빈 목록. 아래 .map 이 터지지 않는다
  const sigunguList = selected ? selected.sigungu : []

  return (
    <div className="address">
      <select
        className="address__select"
        // || '' 로 빈 글자를 넣는다. value 가 undefined 면 리액트가
        // "값을 안 쓰는 입력칸" 으로 보고 경고를 낸다
        value={sido || ''}
        // 시/도를 바꾸면 시/군/구는 비운다 (없는 주소가 만들어지지 않게)
        //
        // 안 비우면 "서울특별시 해운대구" 같은 주소가 만들어진다.
        // 서버가 좌표를 못 찾아 시·도 중심으로 떨어지는데, 사용자는
        // 자기가 고른 것이 맞다고 생각한다
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
        // 시/도는 그대로 두고 시/군/구만 바꾼다
        onChange={(e) => onChange(sido, e.target.value)}
        // 시/도를 안 골랐으면 못 누르게 막는다. 열어두면 빈 목록이
        // 떠서 "왜 아무것도 없지" 가 된다
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
