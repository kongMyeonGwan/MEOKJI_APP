package com.meokji.api.ban.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.meokji.api.ban.vo.UserBanVO;

/**
 * USER_BANS 표에 보내는 SQL.
 *
 * <p>실제 SQL 은 resources/mapper/BanMapper.xml 에 있다. 메서드 이름과 XML 의
 * id 가 같은 것끼리 이어지므로, 이름을 고칠 때는 둘을 같이 고친다.
 *
 * <p>읽는 메서드가 셋인데 모양이 다 다르다 — 쓰는 쪽에서 필요한 모양이
 * 달라서다. 화면에 목록을 뿌릴 때는 VO 전체가 필요하고, 추천에서 걸러낼 때는
 * 이름만 있으면 된다.
 */
@Mapper
public interface BanDAO {

    /**
     * 내 Ban 중 해제하지 않은 것을 등록 순으로.
     *
     * 등록 순으로 고정한 이유 — 순서를 정하지 않으면 DB 사정에 따라 목록이
     * 뜰 때마다 달라진다. 에러는 아니지만 화면에서 재료가 들썩거린다.
     */
    List<UserBanVO> findActiveByUser(@Param("userId") Long userId);

    /**
     * 하나 넣는다. 중복이면 uq_user_bans 에 걸려 DuplicateKeyException 이 난다.
     *
     * {@code @Param} 이 없는 유일한 메서드다. 인자가 하나뿐이라 XML 에서
     * #{userId} 처럼 VO 의 칸을 바로 꺼내 쓸 수 있다.
     *
     * 넣고 나면 ban.id 에 시퀀스가 준 번호가 채워진다(XML 의 selectKey).
     */
    void insert(UserBanVO ban);

    /**
     * id 로 하나. 남의 것인지 판단은 부르는 쪽이 한다.
     *
     * SQL 에 user_id 조건을 넣지 않은 이유 — 조건에 넣으면 "없는 번호" 와
     * "남의 것" 이 똑같이 null 로 돌아와서, 서비스가 둘을 구별할 수 없다.
     * 지금은 둘 다 404 로 맞추고 있지만, 나중에 로그에 남기거나 다르게
     * 처리하려면 구별할 수 있어야 한다.
     */
    UserBanVO findById(@Param("id") Long id);

    /** 해제. 행을 지우지 않고 use_yn='N' 으로 바꾼다. */
    void deactivate(@Param("id") Long id);

    /**
     * 추천 · 레시피에서 쓸 재료 이름만.
     *
     * 알레르기와 싫어함을 가려서 받는다. 알레르기는 대체재료로도 넣으면 안 되고,
     * 싫어함은 빼거나 다른 것으로 바꿔주면 된다 - 취급이 달라서 따로 읽는다.
     *
     * VO 가 아니라 String 목록인 이유 — 쓰는 쪽은 "이 이름이 들어 있는가" 만
     * 본다. id 나 등록일은 쓰지 않으므로 받아올 이유가 없다.
     *
     * @param banType "ALLERGY" 또는 "DISLIKE". 둘을 한 번에 받지 않는다
     */
    List<String> findActiveNames(
            @Param("userId") Long userId, @Param("banType") String banType);
}
