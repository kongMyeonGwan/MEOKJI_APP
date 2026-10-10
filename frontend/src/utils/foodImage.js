// 요리 사진 주소를 만드는 함수.
//
// 사진 100장은 public/images/food 안에 "요리이름.png" 로 들어 있다.
//   public/images/food/김치찌개.png  ->  /images/food/김치찌개.png
//
// 서버(catalog.json)도 같은 주소를 image_url 로 내려준다. 그런데 게시판
// 가데이터(data/sampleBoard.js)처럼 image_url 이 비어 있는 자리도 있어서,
// 그럴 때는 음식 이름으로 주소를 직접 만들어 쓴다.
//
// encodeURIComponent 로 감싸는 이유: 주소에 한글이 그대로 들어가면 브라우저마다
// 다르게 보내는 경우가 있다. '김치찌개' -> '%EA%B9%80%EC%B9%98%EC%B0%8C%EA%B0%9C'

// public 폴더에 둔 이유 — 100장을 src 에 두고 import 하면 빌드가
// 그만큼 무거워지고, 음식 이름으로 골라 쓸 수가 없다(import 는
// 이름을 변수로 받을 수 없다). public 은 주소로 바로 찾는다.
//
// 주소 앞에 '/' 가 붙는다. 상대 경로로 두면 /community/3 같은
// 화면에서 /community/images/... 로 찾아가 깨진다
export const foodImageUrl = (foodName) => {
  // 이름이 없으면 빈 글자. <img src=""> 가 되어 깨진 그림이 안 보인다
  if (!foodName) {
    return ''
  }
  // 사진이 없는 음식이면 404 가 나고 브라우저가 빈 자리를 그린다.
  // 그 자리를 다른 그림으로 메우는 일은 쓰는 쪽(FoodCard)이 한다
  return `/images/food/${encodeURIComponent(foodName)}.png`
}
