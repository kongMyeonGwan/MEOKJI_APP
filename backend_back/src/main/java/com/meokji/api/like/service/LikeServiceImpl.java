package com.meokji.api.like.service;

import org.springframework.stereotype.Service;

<<<<<<< HEAD
/**
 * 찜 목록 · 하트 누르기 · 전부 비우기.
 *
 * <p>표에는 음식 이름만 들어 있다. 분류 · 조리 시간 · 난이도는 catalog.json 에
 * 있어서, 목록을 줄 때 이름으로 찾아 붙인다. 같이 저장하지 않는 이유는
 * 카탈로그가 바뀌면 찜 목록의 정보가 과거 값으로 굳기 때문이다.
 * (식사 기록은 반대로 굳어야 해서 통째로 저장한다 — MealHistoryVO 참고)
 *
 * <p>하트는 "뒤집기" 라 상태가 셋이다 — 처음 찜 · 찜한 것 풀기 · 푼 것 다시 찜.
 * 행을 지우지 않고 use_yn 만 바꾸기 때문에 셋째가 생긴다.
 */
@Service
public class LikeServiceImpl implements LikeService {

    @Autowired
    private LikeDAO likeDAO;

    // 음식 이름 -> catalog.json 의 음식 한 줄
    @Autowired
    private FoodCatalog foodCatalog;

    /**
     * 내 찜 목록.
     *
     * {@code @Transactional} 이 없다. 읽기만 한다.
     */
    @Override
    public LikeListVO getList(UserVO user) {
        List<String> names = likeDAO.findActiveNames(user.getId());

        // DB 에는 음식 이름만 있다. 화면이 카드를 그리려면 분류 · 조리 시간 · 난이도가
        // 있어야 해서, 이름으로 카탈로그를 찾아 붙여준다.
        //
        // SQL 로는 할 수 없는 일이다 - 그 정보가 DB 가 아니라 파일에 있다
        List<FoodVO> items = new ArrayList<>();
        for (String name : names) {
            JsonNode food = foodCatalog.findFood(name);

            if (food == null) {
                // 카탈로그에 없는 이름이다. 사용자가 직접 검색한 이름으로도 찜이 되기
                // 때문에 정상으로 본다. 이름만 담아 보내면 화면이 그 카드만 간단히 그린다.
                //
                // 여기서 건너뛰면(continue) names 와 items 의 개수가 어긋나
                // 화면이 짝을 맞출 수 없게 된다
                FoodVO unknown = new FoodVO();
                unknown.setFoodName(name);
                items.add(unknown);
            } else {
                // JsonNode(파일에서 읽은 모양) -> FoodVO(화면에 나갈 모양).
                // 바꾸는 코드는 추천 기능과 같이 쓴다 (FoodScorer)
                items.add(FoodScorer.toFoodVO(food));
            }
        }

        LikeListVO response = new LikeListVO();
        // names 와 items 를 둘 다 담는다. 같아 보이지만 쓰는 곳이 다르다 -
        // LikeListVO 주석 참고
        response.setNames(names);
        response.setCount(names.size());
        response.setItems(items);
        return response;
    }

    /**
     * 하트 누르기. 지금 상태를 보고 뒤집는다.
     *
     * {@code @Transactional} 이 붙은 이유 — SELECT 로 지금 상태를 보고
     * INSERT 나 UPDATE 를 한 뒤, 개수를 다시 센다. 그 사이에 다른 요청이
     * 끼어들면 돌려주는 개수가 안 맞는다.
     */
    @Override
    @Transactional
    public RecipeLikeVO toggle(UserVO user, String foodName) {
        // 해제한 행까지 같이 찾는다. 그래야 아래 3번이 가능하다
        RecipeLikeVO found = likeDAO.findByUserAndFood(user.getId(), foodName);
        String useYn;

        if (found == null) {
            // 1. 처음 찜하는 음식 → 새로 넣는다
            RecipeLikeVO like = new RecipeLikeVO();
            // 누가 찜했는지는 토큰에서 온 값을 쓴다
            like.setUserId(user.getId());
            like.setFoodName(foodName);
            try {
                likeDAO.insert(like);
            } catch (DuplicateKeyException e) {
                // 두 기기에서 동시에 눌러서 이미 들어간 경우 → 그 행을 켠다
                //
                // 위에서 찾았을 때는 없었는데, 찾기와 넣기 사이에 다른 요청이
                // 먼저 넣은 것이다. DB 의 UNIQUE 가 막아주므로 여기서 받아
                // "그 행을 켜기" 로 바꾼다. 에러로 올려보내면 사용자는
                // 하트를 눌렀는데 아무 일도 안 일어난 것처럼 보인다
                RecipeLikeVO existing = likeDAO.findByUserAndFood(user.getId(), foodName);
                likeDAO.setUseYn(existing.getId(), "Y");
            }
            useYn = "Y";
        } else if (found.getLiked()) {
            // 2. 찜돼 있으면 → 푼다
            // 행을 지우지 않는다. 지우면 "언제 찜했는지" 가 사라져서
            // 다시 찜할 때 목록 맨 앞으로 올라온다
            likeDAO.setUseYn(found.getId(), "N");
            useYn = "N";
        } else {
            // 3. 예전에 찜했다가 푼 음식 → 다시 켠다
            // 새로 넣으면 같은 음식의 행이 쌓인다
            likeDAO.setUseYn(found.getId(), "Y");
            useYn = "Y";
        }

        // 바꾼 뒤의 상태. liked 는 이 use_yn 에서 나온다
        //
        // DB 에서 다시 읽지 않고 직접 만든다. 방금 우리가 정한 값이라
        // 다시 읽어도 같고, 쿼리를 한 번 아낄 수 있다
        RecipeLikeVO response = new RecipeLikeVO();
        response.setFoodName(foodName);
        response.setUseYn(useYn);
        // 개수는 DB 에서 센다. 화면이 1 을 더하거나 빼서 맞추면
        // 다른 기기에서 누른 것과 어긋난다
        response.setCount(likeDAO.countActive(user.getId()));
        return response;
    }

    /**
     * 찜 전부 비우기.
     *
     * {@code @Transactional} 이 붙은 이유 — UPDATE 가 들어간다.
     * 여러 행을 한 SQL 로 바꾸므로 그 자체로 한 묶음이지만,
     * 쓰기를 하는 메서드라는 표시이기도 하다.
     */
    @Override
    @Transactional
    public LikeListVO clear(UserVO user) {
        // 한 SQL 로 전부 use_yn='N' 으로 바꾼다. 목록을 읽어 하나씩 끄면
        // 찜이 30개면 UPDATE 를 30번 보낸다
        likeDAO.deactivateAll(user.getId());

        // 비운 뒤의 상태를 만들어 돌려준다. DB 를 다시 읽지 않는 이유는
        // 방금 전부 껐으므로 결과가 빈 목록임을 이미 알기 때문이다
        LikeListVO response = new LikeListVO();
        // null 이 아니라 빈 목록을 넣는다. 화면이 names.map 을 바로 쓸 수 있다
        response.setNames(new ArrayList<>());
        response.setCount(0);
        // items 는 채우지 않는다 - null 로 나간다. 찜 목록 화면은 비운 뒤에
        // 카드를 그릴 일이 없고, 화면도 count 가 0 이면 빈 상태를 띄운다
        return response;
    }
=======
/** 찜한 레시피. */
@Service
public class LikeServiceImpl implements LikeService {
>>>>>>> 137ab2e5d83abad60912fb1f0b6b8a1315e728eb
}
