package com.meokji.api.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.meokji.api.vo.UserBan;

/** USER_BANS 표에 보내는 SQL. */
@Mapper
public interface BanMapper {

    /** 내 Ban 중 해제하지 않은 것을 등록 순으로. */
    List<UserBan> findActiveByUser(@Param("userId") Long userId);

    /** 하나 넣는다. 중복이면 uq_user_bans 에 걸려 DuplicateKeyException 이 난다. */
    void insert(UserBan ban);

    /** id 로 하나. 남의 것인지 판단은 부르는 쪽이 한다. */
    UserBan findById(@Param("id") Long id);

    /** 해제. 행을 지우지 않고 use_yn='N' 으로 바꾼다. */
    void deactivate(@Param("id") Long id);

    /**
     * 추천 · 레시피에서 쓸 재료 이름만.
     *
     * <p>알레르기와 싫어함을 가려서 받는다. 알레르기는 대체재료로도 넣으면 안 되고,
     * 싫어함은 빼거나 다른 것으로 바꿔주면 된다 - 취급이 달라서 따로 읽는다.
     */
    List<String> findActiveNames(
            @Param("userId") Long userId, @Param("banType") String banType);
}
