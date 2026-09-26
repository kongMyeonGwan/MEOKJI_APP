package com.meokji.api.user;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * USERS 표에 보내는 SQL.
 *
 * <p>SQL 은 {@code resources/mapper/UserMapper.xml} 에 있다. 애노테이션에 적지 않고
 * XML 로 빼둔 이유는, 이 프로젝트의 SQL 이 눈으로 확인해야 하는 것들을 담고 있어서다 -
 * 모든 조회에 {@code use_yn='Y'} 가 붙고, 중복은 함수 기반 UNIQUE 로 걸려 있다.
 */
@Mapper
public interface UserMapper {

    /** id 로 한 명. 탈퇴 회원도 그대로 돌려준다 - 탈퇴 여부 판단은 부르는 쪽이 한다. */
    User findById(@Param("id") Long id);

    /** 이메일로 한 명. 탈퇴하지 않은 회원만 본다. */
    User findActiveByEmail(@Param("email") String email);

    /**
     * 이메일 · 닉네임이 이미 쓰이는지 미리 본다.
     *
     * <p>어느 쪽이 중복인지 사용자에게 알려주려고 미리 조회한다. 이 조회와 INSERT
     * 사이에 다른 요청이 끼어들 수 있어서, 그 경합은 INSERT 쪽에서 한 번 더 잡는다.
     *
     * @return 중복인 행(이메일 · 닉네임만) 또는 null
     */
    User findActiveByEmailOrNickname(
            @Param("email") String email,
            @Param("nickname") String nickname);

    /**
     * 회원 한 명을 넣는다.
     *
     * <p>{@code id} 는 DDL 의 IDENTITY 가 만든다. 넣은 뒤 그 값을 인자 객체의
     * {@code id} 에 되돌려 받는다(XML 의 {@code useGeneratedKeys}).
     */
    void insert(User user);

    /** 로그인 실패 횟수만 올린다. */
    void increaseLoginFailCount(@Param("id") Long id);

    /** 실패 횟수를 0 으로. 로그인에 성공했을 때만 부른다. */
    void resetLoginFailCount(@Param("id") Long id);

    /**
     * 계정을 잠근다.
     *
     * <p>{@code locked_at} 을 SQL 에서 {@code SYS_EXTRACT_UTC(SYSTIMESTAMP)} 로 넣는다.
     * DDL 의 DEFAULT 는 INSERT 시점에만 적용되므로 UPDATE 에서는 직접 넣어야 하고,
     * 애플리케이션에서 만든 시각을 보내면 JVM 시간대에 따라 어긋날 수 있다.
     */
    void lockAccount(@Param("id") Long id);
}
