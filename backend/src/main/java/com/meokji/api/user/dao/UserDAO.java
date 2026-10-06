package com.meokji.api.user.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.meokji.api.user.vo.NotificationVO;
import com.meokji.api.user.vo.UserVO;

/**
 * USERS 표에 보내는 SQL.
 *
 * SQL 은 resources/mapper/UserMapper.xml 에 있다. 애노테이션에 적지 않고
 * XML 로 빼둔 이유는, 이 프로젝트의 SQL 이 눈으로 확인해야 하는 것들을 담고 있어서다 -
 * 모든 조회에 use_yn='Y' 가 붙고, 중복은 함수 기반 UNIQUE 로 걸려 있다.
 */
@Mapper
public interface UserDAO {

    /** id 로 한 명. 탈퇴 회원도 그대로 돌려준다 - 탈퇴 여부 판단은 부르는 쪽이 한다. */
    UserVO findById(@Param("id") Long id);

    /** 이메일로 한 명. 탈퇴하지 않은 회원만 본다. */
    UserVO findActiveByEmail(@Param("email") String email);

    /**
     * 이메일 · 닉네임이 이미 쓰이는지 미리 본다.
     *
     * 어느 쪽이 중복인지 사용자에게 알려주려고 미리 조회한다. 이 조회와 INSERT
     * 사이에 다른 요청이 끼어들 수 있어서, 그 경합은 INSERT 쪽에서 한 번 더 잡는다.
     *
     * @return 중복인 행(이메일 · 닉네임만) 또는 null
     */
    UserVO findActiveByEmailOrNickname(
            @Param("email") String email,
            @Param("nickname") String nickname);

    /**
     * 회원 한 명을 넣는다.
     *
     * id 는 DDL 의 IDENTITY 가 만든다. 넣은 뒤 그 값을 인자 객체의
     * id 에 되돌려 받는다(XML 의 useGeneratedKeys).
     */
    void insert(UserVO user);

    /** 로그인 실패 횟수만 올린다. */
    void increaseLoginFailCount(@Param("id") Long id);

    /** 실패 횟수를 0 으로. 로그인에 성공했을 때만 부른다. */
    void resetLoginFailCount(@Param("id") Long id);

    /**
     * 계정을 잠근다.
     *
     * locked_at 을 SQL 에서 SYS_EXTRACT_UTC(SYSTIMESTAMP) 로 넣는다.
     * DDL 의 DEFAULT 는 INSERT 시점에만 적용되므로 UPDATE 에서는 직접 넣어야 하고,
     * 애플리케이션에서 만든 시각을 보내면 JVM 시간대에 따라 어긋날 수 있다.
     */
    void lockAccount(@Param("id") Long id);

    /**
     * Refresh Token 을 저장한다. 로그인 성공 · 토큰 재발급 때 부른다.
     *
     * 토큰 원문이 아니라 SHA-256 해시를 넣는다. DB 가 새더라도 그 값으로는
     * 로그인할 수 없게 하려는 것이다 (db/11_add_refresh_token.sql 참고).
     *
     * 한 사람당 한 개라서 늘 덮어쓴다 - 폰에서 로그인하면 PC 쪽 토큰이 사라진다.
     */
    void updateRefreshToken(
            @Param("id") Long id,
            @Param("refreshTokenHash") String refreshTokenHash,
            @Param("refreshExpiresAt") LocalDateTime refreshExpiresAt);

    /**
     * Refresh Token 을 지운다 (로그아웃).
     *
     * 쿠키만 지우면 안 된다. 쿠키 값을 미리 복사해둔 사람은 그걸로 계속 재발급을
     * 받을 수 있기 때문이다. 서버 쪽 기록을 NULL 로 되돌려야 진짜로 끝난다.
     */
    void clearRefreshToken(@Param("id") Long id);

    /**
     * 내 정보 수정. null 이 아닌 칸만 고친다.
     *
     * PATCH 라서 보내지 않은 칸은 그대로 두어야 한다. 그 판단은 서비스가 하고
     * (무엇을 보냈는지는 DTO 가 안다), 여기는 받은 값을 그대로 넣는다.
     */
    void updateProfile(UserVO user);

    /** 비밀번호만 바꾼다. */
    void updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    /**
     * 비밀번호를 바꾸면서 계정 잠금도 함께 푼다. 비밀번호 재설정이 쓴다.
     *
     * #updatePassword 와 나누어 둔 이유는, 내 정보에서 비밀번호를 바꾸는
     * 길(로그인한 상태)로는 잠금이 풀려서는 안 되기 때문이다. 잠긴 계정은 애초에
     * 로그인이 안 되므로 그 길로 들어올 수 없지만, 한 메서드로 합쳐두면 나중에
     * 누군가 다른 데서 불러 잠금이 조용히 풀릴 수 있다.
     */
    void resetPasswordAndUnlock(
            @Param("id") Long id, @Param("passwordHash") String passwordHash);

    /** 회원 탈퇴. 행을 지우지 않고 use_yn='N' 으로 바꾼다. */
    void withdraw(@Param("id") Long id);

    /**
     * 탈퇴할 때 딸린 자료도 같이 내린다.
     *
     * 표 이름을 문자열로 받는다. 다섯 표에 같은 UPDATE 를 보내는 일이라
     * 매퍼 메서드를 다섯 개 두는 것보다 낫다. 부르는 쪽이 정해둔 목록에서만
     * 값이 오므로 사용자 입력이 섞일 자리가 없다.
     */
    void withdrawChildren(@Param("table") String table, @Param("userId") Long userId);

    /**
     * 내 글에 달린 댓글.
     *
     * 내가 쓴 댓글은 뺀다 - 자기 글에 자기가 답한 것을 알림으로 받을 이유가 없다.
     */
    List<NotificationVO> findCommentNotifications(
            @Param("userId") Long userId, @Param("limit") int limit);

    /** 내 글이 받은 좋아요. 내가 누른 것은 뺀다. */
    List<NotificationVO> findLikeNotifications(
            @Param("userId") Long userId, @Param("limit") int limit);
}
