import { useState } from 'react'
import FoodIllustration from './FoodIllustration'
import { foodImageUrl } from '../../utils/foodImage'

// 음식 썸네일.
//   1. 서버가 준 사진 주소(image_url)
//   2. 없으면 음식 이름으로 만든 주소 (/images/food/김치찌개.png)
//   3. 그 사진도 없으면 SVG 그림(FoodIllustration)
//
// size 는 'lg'(카드), 'md'(목록), 'sm'(표) 중 하나
//
// 세 단계로 떨어지게 둔 이유 — 음식 100종에 사진이 다 있지는 않고,
// 사용자가 직접 검색한 이름으로도 카드가 그려진다. 어느 단계에서도
// 빈 자리가 보이지 않아야 한다.
//
// 그림(FoodIllustration)이 마지막인 것이 중요하다. 사진이 없을 때
// 회색 네모를 두면 "안 불러와진 것" 처럼 보인다.
export default function FoodThumb({ item, size = 'md' }) {
  // 사진을 못 불러온 음식 이름. 이름으로 담아두는 이유는 월드컵처럼 같은 자리에
  // 다른 음식이 들어오는 화면에서, 앞 음식의 실패가 남지 않게 하려는 것이다.
  const [brokenName, setBrokenName] = useState('')

  const className = `thumb thumb--${size}`
  // 1순위는 서버가 준 주소. 서버가 모르는 음식이면 이름으로 만든다.
  //
  // 서버 쪽을 먼저 보는 이유 — 사진 파일 이름과 음식 이름이 다른
  // 경우가 있을 수 있다. catalog.json 에 적어둔 값이 더 정확하다
  const src = item.image_url ? item.image_url : foodImageUrl(item.food_name)

  // brokenName 과 지금 음식 이름을 견준다.
  // 같으면 "이 음식의 사진은 실패했다" 는 뜻이라 그림으로 넘어간다
  if (src && brokenName !== item.food_name) {
    // onError 는 사진을 못 불러왔을 때 불린다. 그때 이름을
    // 적어두면 다시 그려지면서 아래 그림으로 바뀐다.
    //
    // alt 에 음식 이름을 넣는다. 사진이 안 뜰 때 그 글자가
    // 보이고, 화면 읽어주는 프로그램도 이것을 읽는다
    return (
      <img
        className={className}
        src={src}
        alt={item.food_name}
        onError={() => setBrokenName(item.food_name)}
      />
    )
  }

  // 사진이 없거나 실패한 경우. art 가 없어도 FoodIllustration 이
  // 기본 그림을 그려주므로 여기서 또 확인하지 않는다
  return (
    <div className={className}>
      <FoodIllustration art={item.art} className="thumb__art" />
    </div>
  )
}
