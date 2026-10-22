package com.meokji.api.favorite.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meokji.api.favorite.dto.FavoriteToggleResponse;
import com.meokji.api.favorite.dto.FavoritesResponse;
import com.meokji.api.favorite.entity.FavoriteEntity;
import com.meokji.api.favorite.repository.FavoriteRepository;
import com.meokji.api.recommend.vo.FoodVO;
import com.meokji.api.util.FoodCatalog;
import com.meokji.api.util.FoodScorer;

import tools.jackson.databind.JsonNode;

/**
 * 찜한 레시피. 읽고 쓰는 것은 전부 JPA 다.
 *
 * 트랜잭션은 클래스에 readOnly 로 걸어 두고, 쓰는 메서드에만 @Transactional 을
 * 다시 붙인다 (FreePostService 와 같은 방식).
 *
 * 하는 일은 표 하나를 켜고 끄는 것뿐이라 서비스가 얇다. 그래도 컨트롤러가
 * 레포지토리를 직접 부르지 않게 한 칸을 둔다 — 켜고 끄는 규칙(아래 toggle)이
 * 화면이 아니라 서버 몫이기 때문이다.
 */
@Service
@Transactional(readOnly = true) // 기본은 조회. 쓰는 메서드만 아래에서 다시 건다
public class FavoriteService {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private FoodCatalog catalog;

    /** 내 찜 목록. 최근 찜한 순 */
    public FavoritesResponse getList(Long userId) {
        List<String> names = favoriteRepository.findActiveNames(userId);

        // 찜 표에는 이름만 있다. 화면이 그릴 정보는 카탈로그에서 찾아 채운다
        List<FoodVO> items = new ArrayList<>();

        for (String name : names) {
            items.add(findFood(name));
        }

        return new FavoritesResponse(names, items, names.size());
    }

    /**
     * 찜하기 · 해제. 같은 이름을 다시 보내면 뒤집힌다.
     *
     * 해제한 행을 놔두고 되살리는 방식이다. 새로 INSERT 하면 죽은 행이 쌓인다 —
     * 이유는 FavoriteEntity 주석에 적어 두었다.
     */
    @Transactional
    public FavoriteToggleResponse toggle(Long userId, String foodName) {
        String name = foodName.trim();

        FavoriteEntity favorite = favoriteRepository
                .findFirstByUserIdAndFoodNameOrderByIdDesc(userId, name)
                .orElse(null);

        boolean favorited;

        if (favorite == null) {
            favorited = insertNew(userId, name);
        } else if (favorite.isActive()) {
            favorite.turnOff();
            favorited = false;
        } else {
            favorite.turnOn();
            favorited = true;
        }

        // 개수는 바뀐 뒤의 값이어야 한다. 위에서 바꾼 것은 아직 UPDATE 가 안
        // 나갔지만, 하이버네이트는 쿼리를 보내기 전에 밀린 변경을 먼저
        // 내보내므로(auto flush) 이 숫자는 바뀐 뒤의 값이다
        int count = favoriteRepository.countByUserIdAndUseYn(userId, "Y");

        return new FavoriteToggleResponse(name, favorited, count);
    }

    /** 찜 전체 비우기 (설정 화면) */
    @Transactional
    public FavoritesResponse clear(Long userId) {
        favoriteRepository.deactivateAll(userId);

        return new FavoritesResponse(new ArrayList<>(), new ArrayList<>(), 0);
    }

    /**
     * 처음 찜하는 음식을 넣는다. 항상 true 를 돌려준다.
     *
     * 두 기기에서 같은 하트를 동시에 누르면 둘 다 "찜한 적 없음" 으로 보고
     * INSERT 를 해서, 뒤에 온 쪽이 uq_recipe_likes 에 막힌다. 그때는 먼저
     * 들어간 행을 찾아 켜는 쪽으로 맞춘다 — 사용자가 켜려고 누른 것이므로
     * 결과는 '켜짐' 이어야 한다.
     *
     * saveAndFlush 인 이유: save() 만 하면 INSERT 가 트랜잭션 끝에 나가서
     * 여기서 잡을 수가 없다. 바로 보내야 여기서 받는다.
     */
    private boolean insertNew(Long userId, String name) {
        try {
            favoriteRepository.saveAndFlush(FavoriteEntity.create(userId, name));
        } catch (DataIntegrityViolationException exc) {
            FavoriteEntity existing = favoriteRepository
                    .findFirstByUserIdAndFoodNameOrderByIdDesc(userId, name)
                    .orElse(null);

            if (existing != null) {
                existing.turnOn();
            }
        }

        return true;
    }

    /** 이름으로 카탈로그에서 음식 정보. 없으면 이름만 담아 돌려준다 */
    private FoodVO findFood(String foodName) {
        JsonNode food = catalog.findFood(foodName);

        if (food != null) {
            return FoodScorer.toFoodVO(food);
        }

        // 사용자가 직접 검색한 이름으로도 찜이 남을 수 있다. 그건 정상으로 본다
        FoodVO item = new FoodVO();
        item.setFoodName(foodName);
        return item;
    }
}
