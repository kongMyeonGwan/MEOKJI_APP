package com.meokji.api.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meokji.api.dto.like.LikeListResponse;
import com.meokji.api.dto.like.LikeToggleRequest;
import com.meokji.api.dto.like.LikeToggleResponse;
import com.meokji.api.mapper.LikeMapper;
import com.meokji.api.vo.RecipeLike;
import com.meokji.api.vo.User;

/**
 * 찜한 레시피 (STEP 36).
 *
 * <p>파이썬 쪽 {@code app/services/like_service.py} 자리다. 하는 일은 표 하나를
 * 켜고 끄는 것뿐이지만, <b>컨트롤러 → 서비스 → 매퍼</b> 를 예외 없이 지키려고
 * 서비스를 둔다.
 */
@Service
public class LikeService {

    @Autowired
    private LikeMapper likes;

    /** 내 찜 목록. 최근 찜한 순. */
    public LikeListResponse list(User me) {
        List<String> names = likes.findActiveNames(me.getId());

        return new LikeListResponse(names, names.size());
    }

    /**
     * 찜하기 · 해제. 같은 이름을 다시 보내면 뒤집힌다.
     *
     * <p>지운 행을 남겨두고 되살리는 방식이다. {@code uq_recipe_likes} 가 함수 기반
     * UNIQUE 라 {@code use_yn='Y'} 인 행만 중복을 막는다 - 껐던 행을 놔둔 채 새로
     * INSERT 하면 죽은 행이 계속 쌓인다.
     */
    @Transactional
    public LikeToggleResponse toggle(User me, LikeToggleRequest request) {
        String foodName = request.trimmedFoodName();
        RecipeLike found = likes.findByUserAndFood(me.getId(), foodName);

        boolean liked;

        if (found == null) {
            RecipeLike row = new RecipeLike();
            row.setUserId(me.getId());
            row.setFoodName(foodName);

            try {
                likes.insert(row);
            } catch (DuplicateKeyException exc) {
                // 다른 요청이 먼저 만들었다(두 기기에서 같이 누른 경우).
                // 그 행을 찾아 켜는 쪽으로 맞춘다.
                RecipeLike existing = likes.findByUserAndFood(me.getId(), foodName);
                likes.setUseYn(existing.getId(), "Y");
            }

            liked = true;
        } else {
            liked = !found.isActive();
            likes.setUseYn(found.getId(), liked ? "Y" : "N");
        }

        return new LikeToggleResponse(foodName, liked, likes.countActive(me.getId()));
    }

    /** 찜 전체 비우기 (설정 화면). */
    @Transactional
    public LikeListResponse clear(User me) {
        likes.deactivateAll(me.getId());

        return new LikeListResponse(List.of(), 0);
    }
}
