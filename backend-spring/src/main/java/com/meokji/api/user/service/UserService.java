package com.meokji.api.user.service;

import java.util.List;

import com.meokji.api.user.vo.NotificationItemVO;
import com.meokji.api.user.vo.PasswordChangeVO;
import com.meokji.api.user.vo.ProfileUpdateVO;
import com.meokji.api.user.vo.UserVO;

/** 내 정보 · 알림 · 회원 탈퇴. 실제 코드는 UserServiceImpl 에 있다. */
public interface UserService {

    // 내 정보 (연속 기록일, 요리 횟수 포함)
    UserVO getProfile(UserVO user);

    // 내 정보 수정
    UserVO updateProfile(UserVO user, ProfileUpdateVO request);

    // 비밀번호 변경
    void changePassword(UserVO user, PasswordChangeVO request);

    // 내 글에 달린 댓글 · 좋아요 알림
    List<NotificationItemVO> getNotifications(UserVO user);

    // 회원 탈퇴
    void withdraw(UserVO user, String password);
}
