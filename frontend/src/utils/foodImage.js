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

export const foodImageUrl = (foodName) => {
  if (!foodName) {
    return ''
  }
  return `/images/food/${encodeURIComponent(foodName)}.png`
}
