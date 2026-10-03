// 자유게시판 퍼블리싱용 임시 데이터.
//
// ! 이 파일은 담당자가 API 를 붙이면 통째로 지운다.
//
// 자유게시판 서버는 아직 없다. 그래도 화면 모양은 확인할 수 있어야 해서,
// 서버에서 받아올 값을 여기에 같은 모양으로 적어두고 화면이 이걸 그리게 해놨다.
//
// 필드 이름은 DB 의 FREE_POSTS 컬럼과 똑같다 (db/09_add_free_posts.sql).
// 그래서 나중에 이 배열 대신 API 응답을 넣기만 하면 화면은 안 고쳐도 된다.
//
// 레시피 후기(sampleBoard.js)와 다른 점
//   - food_name · recipe_json 이 없다. 자유게시판은 식사 기록에서 파생되지 않는다.
//   - like_count · comment_count 가 없다. 좋아요 · 댓글 표를 만들지 않았다.

// 첨부 사진 미리보기용 임시 그림.
// 서버가 생기면 file.url 에 '/api/free-posts/1/file' 같은 주소가 들어온다.
const SAMPLE_PHOTO =
  "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='480' height='300'>" +
  "<rect width='480' height='300' fill='%23e8e2d6'/>" +
  "<circle cx='240' cy='130' r='56' fill='%23cfc4ae'/>" +
  "<rect x='150' y='196' width='180' height='14' rx='7' fill='%23cfc4ae'/></svg>"

// ===== 글 목록 (GET /api/free-posts) =====
// 서버는 { items, total, page, size, total_pages } 모양으로 준다.
export const SAMPLE_FREE_POSTS = [
  {
    id: 1,
    title: '혼밥하기 괜찮은 식당 공유해요',
    content:
      '회사 근처에 혼자 가도 눈치 안 보이는 곳이 생각보다 많더라고요. 바 자리가 있는 집은 거의 괜찮았습니다. 다들 자주 가는 곳 있으면 알려주세요.',
    nickname: '혼밥선수',
    view_count: 214,
    created_at: '2026-10-02T11:30:00Z',
    has_file: true,
  },
  {
    id: 2,
    title: '냉장고에 두부만 세 모 남았습니다',
    content:
      '유통기한이 모레인데 뭘 해야 할까요. 부침은 어제 했고 찌개는 좀 질렸어요. 두부로 할 수 있는 거 아무거나 받습니다.',
    nickname: '두부부자',
    view_count: 96,
    created_at: '2026-10-02T02:15:00Z',
    has_file: false,
  },
  {
    id: 3,
    title: '편의점 신상 중에 이건 꼭 드셔보세요',
    content:
      '이름은 안 쓰겠지만 노란 봉지에 든 거요. 라면에 넣어도 되고 그냥 먹어도 됩니다. 한 봉지에 두 번 먹을 양이라 가성비도 괜찮아요.',
    nickname: '편의점털이',
    view_count: 377,
    created_at: '2026-10-01T14:40:00Z',
    has_file: false,
  },
  {
    id: 4,
    title: '자취 2년 만에 산 물건 중에 제일 잘 산 것',
    content:
      '전기 그릴이요. 설거지가 프라이팬보다 훨씬 편하고 기름도 덜 튑니다. 고기 말고 야채 구울 때도 쓸 만해요.',
    nickname: '살림초보',
    view_count: 158,
    created_at: '2026-09-30T20:05:00Z',
    has_file: false,
  },
  {
    id: 5,
    title: '다들 밥은 몇 인분씩 해두시나요',
    content:
      '한 번에 많이 해서 냉동했다가 먹으려는데 얼마나 해둬야 할지 모르겠어요. 2인분씩 소분하면 금방 떨어지고, 많이 하면 맛이 떨어지는 느낌입니다.',
    nickname: '밥솥지킴이',
    view_count: 73,
    created_at: '2026-09-29T09:20:00Z',
    has_file: false,
  },
]

// ===== 글 상세 (GET /api/free-posts/{id}) =====
// 목록 항목에 본문 전체와 첨부파일이 더 붙는다.
export const SAMPLE_FREE_POST_DETAIL = {
  id: 1,
  title: '혼밥하기 괜찮은 식당 공유해요',
  content:
    '회사 근처에 혼자 가도 눈치 안 보이는 곳이 생각보다 많더라고요.\n바 자리가 있는 집은 거의 괜찮았습니다.\n\n국밥집은 애초에 혼자 오는 사람이 많아서 제일 편하고, 요즘은 1인 화로 두는 고깃집도 늘었어요.\n다들 자주 가는 곳 있으면 알려주세요.',
  nickname: '혼밥선수',
  view_count: 214,
  created_at: '2026-10-02T11:30:00Z',
  updated_at: '2026-10-02T11:30:00Z',

  // 첨부 사진 한 장. 없으면 null 이다.
  //   url      <img src> 에 넣을 주소
  //   name     사용자가 올린 원래 파일명 (DB 의 FILE_ORG_NAME)
  //   size_text 서버가 '184 KB' 처럼 만들어서 준다
  file: {
    url: SAMPLE_PHOTO,
    name: '혼밥_국밥집.jpg',
    size_text: '184 KB',
  },

  // 내가 쓴 글인지. 'Y' 면 수정 · 삭제 버튼이 보인다.
  // true 로 바꿔두면 수정 폼과 첨부 버튼 모양을 확인할 수 있다.
  is_mine: false,
}
