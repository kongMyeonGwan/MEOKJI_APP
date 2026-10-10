package com.meokji.api.ban.service;

import java.util.List;

import com.meokji.api.ban.vo.UserBanVO;
import com.meokji.api.user.vo.UserVO;

/**
 * 못 먹는 재료. 실제 코드는 BanServiceImpl 에 있다.
 *
 * <p>세 메서드가 모두 첫 인자로 UserVO 를 받는다. "누가 하는 일인지" 가
 * 이 기능의 모든 판단에 들어가기 때문이다 — 내 목록만 보고, 내 것으로 넣고,
 * 내 것만 지운다.
 *
 * <p>Long userId 가 아니라 UserVO 를 받는 이유 — 번호만 받으면 서비스가
 * "이 번호가 정말 로그인한 사람인지" 를 알 수 없다. UserVO 는 AuthService 가
 * 토큰을 확인해 만들어 준 것이라, 받는 쪽은 믿고 쓸 수 있다.
 */
public interface BanService {

    // 내 목록
    // 해제한 것(use_yn = 'N')은 빠진다
    List<UserBanVO> getList(UserVO user);

    // 추가. 이미 등록한 재료면 409 에러
    // 새로 만든 행을 돌려준다 (id · created_at 이 채워진 상태)
    UserBanVO add(UserVO user, UserBanVO request);

    // 삭제. 내 것이 아니면 404 에러
    // 행을 지우는 게 아니라 use_yn 을 'N' 으로 바꾼다
    void delete(UserVO user, Long banId);
}
