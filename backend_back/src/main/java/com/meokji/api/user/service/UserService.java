package com.meokji.api.user.service;

import java.util.List;

import com.meokji.api.user.vo.NotificationItemVO;
import com.meokji.api.user.vo.PasswordChangeVO;
import com.meokji.api.user.vo.ProfileUpdateVO;
import com.meokji.api.user.vo.ProfileVO;
import com.meokji.api.user.vo.UserVO;

/**
 * 내 정보 · 알림 · 회원 탈퇴. 실제 코드는 UserServiceImpl 에 있다.
 *
 * <p>auth 기능과 표를 같이 쓴다(USERS). 나눈 기준은 "로그인 전인가 후인가" 다 -
 * auth 는 아직 로그인하지 않은 사람을 들여보내고, 이쪽은 이미 로그인한
 * 사람의 정보를 다룬다. 그래서 메서드마다 UserVO 를 받는다.
 *
 * <p>비밀번호 바꾸기가 두 군데 있는 것도 그래서다 — 잊어버린 사람은
 * PasswordResetService(이메일 인증), 알고 있는 사람은 이쪽이다.
 */
public interface UserService {

    /**
     * 내 정보 (연속 기록일, 요리 횟수 포함)
     *
     * UserVO 를 이미 받았는데 또 조회하는 모양인 이유 — 연속 기록일과
     * 요리 횟수는 USERS 에 없는 값이다. 식사 기록을 세어야 나온다
     */
    ProfileVO getProfile(UserVO user);

    /**
     * 내 정보 수정
     *
     * 보내지 않은 칸은 바꾸지 않는다(PATCH). 주소를 바꾸면 좌표도
     * 다시 찾아 같이 저장한다
     */
    ProfileVO updateProfile(UserVO user, ProfileUpdateVO request);

    /** 비밀번호 변경. 지금 비밀번호가 틀리면 에러 */
    void changePassword(UserVO user, PasswordChangeVO request);

    /**
     * 내 글에 달린 댓글 · 좋아요 알림
     *
     * 알림 표가 없다. 댓글과 좋아요를 그때그때 읽어 만든다 -
     * 그래서 "읽음 표시" 를 할 수 없다 (NotificationItemVO 주석 참고)
     */
    List<NotificationItemVO> getNotifications(UserVO user);

    /**
     * 회원 탈퇴
     *
     * 행을 지우지 않고 use_yn 을 N 으로 바꾼다. 지우면 그 사람이 쓴
     * 글 · 댓글 · 좋아요가 가리킬 곳을 잃는다
     */
    void withdraw(UserVO user, String password);
}
