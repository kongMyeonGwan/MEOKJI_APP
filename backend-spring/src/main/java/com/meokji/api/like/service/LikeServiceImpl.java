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
import com.meokji.api.user.vo.UserVO;

@Service
public class LikeServiceImpl implements LikeService {

    @Autowired
    private LikeDAO likeDAO;

    @Override
    public LikeListVO getList(UserVO user) {
        List<String> names = likeDAO.findActiveNames(user.getId());

        LikeListVO response = new LikeListVO();
        response.setNames(names);
        response.setCount(names.size());
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
