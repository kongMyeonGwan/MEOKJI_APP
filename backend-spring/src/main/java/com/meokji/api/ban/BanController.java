package com.meokji.api.ban;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.ban.dto.BanCreateRequest;
import com.meokji.api.ban.dto.BanResponse;
import com.meokji.api.security.LoginUser;
import com.meokji.api.user.User;

import jakarta.validation.Valid;

/**
 * 못 먹는 재료(Ban).
 *
 * <p>여기 등록한 재료는 추천과 레시피에서 제외된다. 파이썬 쪽
 * {@code app/routers/bans.py} 자리다.
 */
@RestController
@RequestMapping("/api/bans")
public class BanController {

    @Autowired
    private BanService banService;


    @GetMapping
    public List<BanResponse> list(@LoginUser User me) {
        List<BanResponse> items = new ArrayList<>();

        for (UserBan ban : banService.list(me)) {
            items.add(BanResponse.from(ban));
        }

        return items;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BanResponse create(
            @LoginUser User me,
            @Valid @RequestBody BanCreateRequest request) {

        return BanResponse.from(banService.create(me, request));
    }

    @DeleteMapping("/{banId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@LoginUser User me, @PathVariable Long banId) {
        banService.delete(me, banId);
    }
}
