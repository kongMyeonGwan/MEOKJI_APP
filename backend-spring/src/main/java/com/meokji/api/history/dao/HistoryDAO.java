package com.meokji.api.history.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.meokji.api.history.vo.MealHistoryVO;

/**
 * MEAL_HISTORIES 표에 보내는 SQL.
 *
 * 지금은 요약값(연속 방문일 · 요리 횟수)만 쓴다. 내 정보 화면이 그 값을 같이
 * 보여주기 때문에 히스토리 기능보다 먼저 필요했다. 목록 · 확정 · 삭제는 다음
 * 단계에서 이 매퍼에 붙인다.
 */
@Mapper
public interface HistoryDAO {

    /** 요약을 계산할 재료를 가져온다. */
    List<MealHistoryVO> findStatsRows(@Param("userId") Long userId);

    /**
     * 조건에 맞는 전체 건수.
     *
     * 목록과 같은 조건 · 같은 JOIN 을 봐야 한다. 한쪽에만 필터를 붙이면
     * "3페이지가 있다는데 열면 비어 있는" 일이 생긴다.
     */
    int countByUser(@Param("userId") Long userId, @Param("shared") Boolean shared);

    /** 내 기록 한 페이지. 최신순. */
    List<MealHistoryVO> findPageByUser(
            @Param("userId") Long userId,
            @Param("shared") Boolean shared,
            @Param("offset") int offset,
            @Param("size") int size);

    /** id 로 하나. 남의 것인지 · 지웠는지 판단은 부르는 쪽이 한다. */
    MealHistoryVO findById(@Param("id") Long id);

    /** 식사 확정. id · selected_at 은 DB 가 채운다. */
    void insert(MealHistoryVO history);

    /** 삭제. 행을 지우지 않고 use_yn='N'. */
    void deactivate(@Param("id") Long id);

    /**
     * 이 기록으로 쓴 후기 id. 없으면 null.
     *
     * 기록을 지우려 할 때 확인한다. 공유한 기록을 지우면 후기가 근거를 잃는다.
     */
    Long findSharedReviewId(@Param("historyId") Long historyId);
}
