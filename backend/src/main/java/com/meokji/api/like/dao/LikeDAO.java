package com.meokji.api.like.dao;

import org.apache.ibatis.annotations.Mapper;

<<<<<<< HEAD
/**
 * RECIPE_LIKES 표에 보내는 SQL. 찜한 레시피 (STEP 36).
 *
 * 찜 목록이 원래 브라우저(localStorage)에 있었다. 기기를 바꾸면 안 따라오고,
 * 한 브라우저를 여러 사람이 쓰면 앞사람 찜이 보여서 표로 옮겼다.
 *
 * <p>이 표는 행을 지우지 않는다. 해제도 use_yn='N' 이다. 그래서 읽는
 * 메서드가 "켜진 것만" 과 "꺼진 것까지" 로 나뉘어 있다 - 목록은 켜진 것만
 * 보여주고, 다시 찜할 때는 꺼진 행을 찾아 되살려야 한다.
 *
 * <p>실제 SQL 은 resources/mapper/LikeMapper.xml 에 있다.
 */
@Mapper
public interface LikeDAO {

    /**
     * 내 찜 이름 목록. 최근 찜한 순.
     *
     * VO 가 아니라 String 목록인 이유 — 쓰는 쪽은 이름만 본다. id ·
     * created_at 은 응답에 나가지 않으므로 받아올 이유가 없다.
     *
     * 최근 순으로 고정한 이유 — 정렬을 정하지 않으면 목록이 뜰 때마다
     * 순서가 달라져 화면에서 카드가 들썩거린다.
     */
    List<String> findActiveNames(@Param("userId") Long userId);

    /** 내 찜 개수. use_yn='N' 은 빼야 한다 - 해제한 것도 행은 남아 있다. */
    // findActiveNames 를 받아 size() 를 세지 않는 이유 — toggle 은 개수만
    // 필요하다. 이름을 30개 읽어 와 세는 것보다 COUNT 한 줄이 싸다
    int countActive(@Param("userId") Long userId);

    /**
     * 한 음식의 찜 행. 해제한 것도 함께 찾는다.
     *
     * 해제한 행을 찾아야 다시 찜할 때 그 행을 되살릴 수 있다. 못 찾고
     * 새로 넣으면 함수 기반 UNIQUE 에는 안 걸리지만 같은 음식의 행이 쌓인다.
     *
     * 돌려주는 VO 의 useYn 으로 "지금 켜져 있는지" 를 가린다. 서비스의
     * 세 갈래(처음 찜 · 풀기 · 다시 찜)가 이 값에서 나온다.
     */
    RecipeLikeVO findByUserAndFood(
            @Param("userId") Long userId, @Param("foodName") String foodName);

    /**
     * 찜 한 줄 넣기. use_yn · created_at 은 DDL 의 DEFAULT 가 채운다.
     *
     * 넣는 사이에 다른 요청이 먼저 넣으면 UNIQUE 에 걸려
     * DuplicateKeyException 이 난다. 서비스가 그것을 받아 처리한다.
     */
    void insert(RecipeLikeVO like);

    /** 켜고 끄기. 행을 지우지 않고 use_yn 만 뒤집는다. */
    // 'Y'/'N' 을 인자로 받는다. setActive(id) · setInactive(id) 로 나누면
    // 같은 UPDATE 가 XML 에 두 벌 생긴다
    void setUseYn(@Param("id") Long id, @Param("useYn") String useYn);

    /** 전부 해제 (설정 화면의 '찜 비우기'). */
    // 한 SQL 로 여러 행을 바꾼다. 목록을 읽어 setUseYn 을 하나씩 부르면
    // 찜이 30개면 UPDATE 를 30번 보낸다
    void deactivateAll(@Param("userId") Long userId);
=======
/** RECIPE_LIKES 테이블 (찜한 레시피). */
@Mapper
public interface LikeDAO {
>>>>>>> 137ab2e5d83abad60912fb1f0b6b8a1315e728eb
}
