package com.meokji.api.ban.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.auth.service.AuthService;
import com.meokji.api.ban.service.BanService;
import com.meokji.api.ban.vo.UserBanVO;
import com.meokji.api.user.vo.UserVO;

import jakarta.validation.Valid;

/**
 * 못 먹는 재료.  주소: /api/bans
 * 여기 등록한 재료는 AI 추천과 레시피에서 빠진다. 로그인이 필요하다.
 *
 * <p>목록 · 추가 · 삭제 셋뿐이고 수정이 없다. 재료 이름을 고치는 일은
 * "지우고 다시 넣기" 와 같아서, 화면에서도 그렇게 쓴다.
 *
 * <p>세 메서드가 모두 첫 줄에서 getLoginUser 를 부른다
 *
 * <p>목록은 "내가 등록한 것" 만 보여야 하고, 추가는 "내 것" 으로 저장해야 하고,
 * 삭제는 "내 것인지" 확인해야 한다. 어느 쪽도 로그인 없이는 할 수 없다.
 * 회원 번호를 화면이 보내게 하면 번호만 바꿔 남의 목록을 볼 수 있으므로,
 * 누구인지는 토큰에서만 정한다.
 */
@RestController
@RequestMapping("/api/bans")
public class BanController {

    @Autowired
    private AuthService authService;

    @Autowired
    private BanService banService;

    // 내 목록  GET /api/bans
    //
    // 페이징이 없다. 못 먹는 재료는 많아도 수십 개라 한 번에 다 보내도 된다.
    // 화면도 목록을 접지 않고 다 펼쳐 보여준다
    @GetMapping
    public List<UserBanVO> getList(@RequestHeader(value = "Authorization", required = false) String token) {
        UserVO loginUser = authService.getLoginUser(token);
        // 회원 번호가 아니라 UserVO 를 그대로 넘긴다. 서비스가 다른 칸
        // (닉네임 등)을 쓸 일이 생겨도 메서드 모양을 안 고친다
        return banService.getList(loginUser);
    }

    // 추가  POST /api/bans
    //
    // 받는 것과 돌려주는 것이 같은 UserBanVO 다. 받을 때는 ingredientName 과
    // banType 만 쓰고, 돌려줄 때는 id · created_at 까지 채워 보낸다.
    // userId 는 @JsonIgnore 라 화면이 보내도 무시된다 - 토큰으로만 정한다
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // 201
    public UserBanVO add(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody UserBanVO request) {
        UserVO loginUser = authService.getLoginUser(token);
        // 새로 만든 행을 돌려준다. 화면은 이것을 목록에 바로 끼워 넣어서,
        // 목록을 다시 불러오지 않고도 방금 넣은 재료를 보여준다
        return banService.add(loginUser, request);
    }

    // 삭제  DELETE /api/bans/3
    //
    // 204(내용 없음)를 주는 이유 — 지운 뒤에 돌려줄 값이 없다.
    // 빈 JSON({})을 200 으로 주면 화면이 "뭘 확인해야 하지" 를 따져야 한다
    //
    // 메서드가 void 인데 @ResponseStatus 가 없으면 200 + 빈 본문이 나간다
    @DeleteMapping("/{banId}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void delete(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long banId) {
        UserVO loginUser = authService.getLoginUser(token);
        // 남의 것을 지우려 하면 서비스가 404 를 던진다.
        // 403 이 아니라 404 인 이유는 서비스 쪽 주석에 적어 두었다
        banService.delete(loginUser, banId);
    }
}
