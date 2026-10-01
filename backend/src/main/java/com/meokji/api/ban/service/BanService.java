package com.meokji.api.ban.service;

import java.util.List;

import com.meokji.api.ban.vo.UserBanVO;
import com.meokji.api.user.vo.UserVO;

/** 못 먹는 재료. 실제 코드는 BanServiceImpl 에 있다. */
public interface BanService {

    // 내 목록
    List<UserBanVO> getList(UserVO user);

    // 추가. 이미 등록한 재료면 409 에러
    UserBanVO add(UserVO user, UserBanVO request);

    // 삭제. 내 것이 아니면 404 에러
    void delete(UserVO user, Long banId);
}
