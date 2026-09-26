package com.meokji.api.like;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * RECIPE_LIKES 표에 보내는 SQL. 찜한 레시피 (STEP 36).
 *
 * <p>찜 목록이 원래 브라우저(localStorage)에 있었다. 기기를 바꾸면 안 따라오고,
 * 한 브라우저를 여러 사람이 쓰면 앞사람 찜이 보여서 표로 옮겼다.
 */
@Mapper
public interface LikeMapper {

    /** 내 찜 이름 목록. 최근 찜한 순. */
    List<String> findActiveNames(@Param("userId") Long userId);

    /** 내 찜 개수. use_yn='N' 은 빼야 한다 - 해제한 것도 행은 남아 있다. */
    int countActive(@Param("userId") Long userId);

    /**
     * 한 음식의 찜 행. 해제한 것도 함께 찾는다.
     *
     * <p>해제한 행을 찾아야 <b>다시 찜할 때 그 행을 되살릴</b> 수 있다. 못 찾고
     * 새로 넣으면 함수 기반 UNIQUE 에는 안 걸리지만 같은 음식의 행이 쌓인다.
     */
    RecipeLike findByUserAndFood(
            @Param("userId") Long userId, @Param("foodName") String foodName);

    void insert(RecipeLike like);

    /** 켜고 끄기. 행을 지우지 않고 use_yn 만 뒤집는다. */
    void setUseYn(@Param("id") Long id, @Param("useYn") String useYn);

    /** 전부 해제 (설정 화면의 '찜 비우기'). */
    void deactivateAll(@Param("userId") Long userId);
}
