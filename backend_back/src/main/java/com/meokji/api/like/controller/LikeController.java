package com.meokji.api.like.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.like.service.LikeService;

<<<<<<< HEAD
import jakarta.validation.Valid;

/**
 * 찜한 레시피.  주소: /api/likes
 * 로그인이 필요하다.
 *
 * <p>주소가 /api/likes 하나뿐이고 메서드(GET · PUT · DELETE)로 일을 가린다.
 * /api/likes/{id} 같은 주소가 없는 이유 — 찜은 "내가 이 음식을 찜했다" 는
 * 사실이라, 번호로 가리킬 일이 없다. 음식 이름이 곧 그 찜이다.
 *
 * <p>POST 가 아니라 PUT 인 이유
 *
 * <p>하트는 두 번 누르면 원래대로 돌아온다. POST 는 "새로 만든다" 는 뜻이라
 * 두 번 누르면 두 개가 생기는 것처럼 읽힌다. PUT 은 "이 상태로 맞춰라" 는
 * 뜻이라 여러 번 보내도 결과가 같다는 쪽에 가깝다.
 *
 * <p>개별 삭제가 없다. 하트를 다시 누르면 풀리므로 PUT 하나로 끝난다.
 * DELETE 는 설정 화면의 "찜 비우기" 전용이다.
 */
=======
/** 찜한 레시피.  주소: /api/likes */
>>>>>>> 137ab2e5d83abad60912fb1f0b6b8a1315e728eb
@RestController
@RequestMapping("/api/likes")
public class LikeController {

    @Autowired
    private LikeService likeService;

    // 내 찜 목록  GET /api/likes
    //
    // 페이징이 없다. 찜은 많아도 수십 개라 한 번에 다 보낸다.
    // 응답에 이름 목록과 음식 정보가 같이 들어간다 - LikeListVO 주석 참고
    @GetMapping
<<<<<<< HEAD
    public LikeListVO getList(@RequestHeader(value = "Authorization", required = false) String token) {
        UserVO loginUser = authService.getLoginUser(token);
        return likeService.getList(loginUser);
    }

    // 하트 누르기 (찜돼 있으면 풀고, 아니면 찜한다)  PUT /api/likes
    //
    // 화면이 "켜라/꺼라" 를 보내지 않고 서버가 지금 상태를 보고 뒤집는다.
    // 화면이 정하면, 다른 기기에서 이미 푼 찜을 또 풀려고 보낼 수 있다.
    //
    // trim 을 컨트롤러에서 하는 이유 — 서비스가 이름으로 DB 를 찾는데,
    // " 김치찌개" 로 들어오면 "김치찌개" 와 다른 음식이 된다.
    // 받는 즉시 다듬어 넘기면 서비스는 깨끗한 값만 다룬다
    @PutMapping
    public RecipeLikeVO toggle(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody RecipeLikeVO request) {
        UserVO loginUser = authService.getLoginUser(token);
        // 바뀐 뒤의 상태(liked)와 전체 개수를 돌려준다.
        // 화면은 이 값으로 하트 모양과 "찜 N개" 를 한 번에 맞춘다
        return likeService.toggle(loginUser, request.getFoodName().trim());
    }

    // 찜 전부 지우기  DELETE /api/likes
    //
    // 204 가 아니라 200 + 빈 목록을 주는 이유 — 화면이 목록을 다시 불러오지
    // 않고 받은 것으로 바로 비울 수 있다. count 가 0 인 것도 같이 온다
    @DeleteMapping
    public LikeListVO clear(@RequestHeader(value = "Authorization", required = false) String token) {
        UserVO loginUser = authService.getLoginUser(token);
        return likeService.clear(loginUser);
=======
    public void getList() {
>>>>>>> 137ab2e5d83abad60912fb1f0b6b8a1315e728eb
    }
}
