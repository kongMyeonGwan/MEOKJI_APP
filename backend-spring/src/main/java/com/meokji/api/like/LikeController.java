package com.meokji.api.like;

import org.springframework.beans.factory.annotation.Autowired;
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
 * <p>파이썬 쪽 {@code app/routers/likes.py} 자리다. 켜고 끄는 규칙은
 * {@link LikeService} 에 있다.
 */
@RestController
@RequestMapping("/api/likes")
public class LikeController {

    @Autowired
    private LikeService likeService;

    /** 내 찜 목록. 최근 찜한 순. */
    @GetMapping
    public LikeListResponse list(@LoginUser User me) {
        return likeService.list(me);
    }

    /**
     * 찜하기 · 해제. 같은 이름을 다시 보내면 뒤집힌다.
     *
     * <p>PUT 인데 멱등이 아니다. 하트 버튼 하나로 켜고 끄는 동작이라 화면이 지금
     * 상태를 몰라도 되게 만든 것인데, 그 대신 같은 요청을 두 번 보내면 원래대로
     * 돌아온다. 화면이 재시도하지 않는다는 전제가 깔려 있다.
     */
    @PutMapping
    public LikeToggleResponse toggle(
            @LoginUser User me,
            @Valid @RequestBody LikeToggleRequest request) {

        return likeService.toggle(me, request);
    }

    /** 찜 전체 비우기 (설정 화면). */
    @DeleteMapping
    public LikeListResponse clear(@LoginUser User me) {
        return likeService.clear(me);
    }
}
