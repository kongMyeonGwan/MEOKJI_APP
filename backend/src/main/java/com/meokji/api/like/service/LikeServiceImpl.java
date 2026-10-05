package com.meokji.api.like.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meokji.api.like.dao.LikeDAO;
import com.meokji.api.like.vo.LikeListVO;
import com.meokji.api.like.vo.LikeToggleVO;
import com.meokji.api.like.vo.RecipeLikeVO;
import com.meokji.api.recommend.vo.FoodVO;
import com.meokji.api.user.vo.UserVO;
import com.meokji.api.util.FoodCatalog;
import com.meokji.api.util.FoodScorer;

import tools.jackson.databind.JsonNode;

@Service
public class LikeServiceImpl implements LikeService {

    @Autowired
    private LikeDAO likeDAO;

    @Autowired
    private FoodCatalog foodCatalog;

    @Override
    public LikeListVO getList(UserVO user) {
        List<String> names = likeDAO.findActiveNames(user.getId());

        // DB 에는 음식 이름만 있다. 화면이 카드를 그리려면 분류 · 조리 시간 · 난이도가
        // 있어야 해서, 이름으로 카탈로그를 찾아 붙여준다.
        List<FoodVO> items = new ArrayList<>();
        for (String name : names) {
            JsonNode food = foodCatalog.findFood(name);

            if (food == null) {
                // 카탈로그에 없는 이름이다. 사용자가 직접 검색한 이름으로도 찜이 되기
                // 때문에 정상으로 본다. 이름만 담아 보내면 화면이 그 카드만 간단히 그린다.
                FoodVO unknown = new FoodVO();
                unknown.setFoodName(name);
                items.add(unknown);
            } else {
                items.add(FoodScorer.toFoodVO(food));
            }
        }

        LikeListVO response = new LikeListVO();
        response.setNames(names);
        response.setCount(names.size());
        response.setItems(items);
        return response;
    }

    @Override
    @Transactional
    public LikeToggleVO toggle(UserVO user, String foodName) {
        RecipeLikeVO found = likeDAO.findByUserAndFood(user.getId(), foodName);
        boolean liked;

        if (found == null) {
            // 1. 처음 찜하는 음식 → 새로 넣는다
            RecipeLikeVO like = new RecipeLikeVO();
            like.setUserId(user.getId());
            like.setFoodName(foodName);
            try {
                likeDAO.insert(like);
            } catch (DuplicateKeyException e) {
                // 두 기기에서 동시에 눌러서 이미 들어간 경우 → 그 행을 켠다
                RecipeLikeVO existing = likeDAO.findByUserAndFood(user.getId(), foodName);
                likeDAO.setUseYn(existing.getId(), "Y");
            }
            liked = true;
        } else if ("Y".equals(found.getUseYn())) {
            // 2. 찜돼 있으면 → 푼다
            likeDAO.setUseYn(found.getId(), "N");
            liked = false;
        } else {
            // 3. 예전에 찜했다가 푼 음식 → 다시 켠다
            likeDAO.setUseYn(found.getId(), "Y");
            liked = true;
        }

        LikeToggleVO response = new LikeToggleVO();
        response.setFoodName(foodName);
        response.setLiked(liked);
        response.setCount(likeDAO.countActive(user.getId()));
        return response;
    }

    @Override
    @Transactional
    public LikeListVO clear(UserVO user) {
        likeDAO.deactivateAll(user.getId());

        LikeListVO response = new LikeListVO();
        response.setNames(new ArrayList<>());
        response.setCount(0);
        return response;
    }
}
