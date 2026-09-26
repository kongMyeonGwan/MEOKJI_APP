package com.meokji.api.like;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.like.dto.LikeListResponse;
import com.meokji.api.like.dto.LikeToggleRequest;
import com.meokji.api.like.dto.LikeToggleResponse;
import com.meokji.api.security.LoginUser;
import com.meokji.api.user.User;

import jakarta.validation.Valid;

/**
 * 찜한 레시피 (STEP 36).
 *
 * <p>파이썬 쪽 {@code app/routers/likes.py} + {@code services/like_service.py} 를
 * 합쳤다. 하는 일이 표 한 개를 켜고 끄는 것이라 서비스 클래스를 따로 두지 않았다.
 */
@RestController
@RequestMapping("/api/likes")
public class LikeController {

    private final LikeMapper likes;

    public LikeController(LikeMapper likes) {
        this.likes = likes;
    }

    /** 내 찜 목록. 최근 찜한 순. */
    @GetMapping
    public LikeListResponse list(@LoginUser User me) {
        List<String> names = likes.findActiveNames(me.getId());

        return new LikeListResponse(names, names.size());
    }

    /**
     * 찜하기 · 해제. 같은 이름을 다시 보내면 뒤집힌다.
     *
     * <p>PUT 인데 멱등이 아니다. 하트 버튼 하나로 켜고 끄는 동작이라 화면이 지금
     * 상태를 몰라도 되게 만든 것인데, 그 대신 같은 요청을 두 번 보내면 원래대로
     * 돌아온다. 화면이 재시도하지 않는다는 전제가 깔려 있다.
     */
    @PutMapping
    @Transactional
    public LikeToggleResponse toggle(
            @LoginUser User me,
            @Valid @RequestBody LikeToggleRequest request) {

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
    @DeleteMapping
    @Transactional
    public LikeListResponse clear(@LoginUser User me) {
        likes.deactivateAll(me.getId());

        return new LikeListResponse(List.of(), 0);
    }
}
