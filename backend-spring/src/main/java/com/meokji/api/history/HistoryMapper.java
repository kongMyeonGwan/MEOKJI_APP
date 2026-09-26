package com.meokji.api.history;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MEAL_HISTORIES 표에 보내는 SQL.
 *
 * <p>지금은 요약값(연속 방문일 · 요리 횟수)만 쓴다. 내 정보 화면이 그 값을 같이
 * 보여주기 때문에 히스토리 기능보다 먼저 필요했다. 목록 · 확정 · 삭제는 다음
 * 단계에서 이 매퍼에 붙인다.
 */
@Mapper
public interface HistoryMapper {

    /** 요약 계산용 한 행. 날짜와 비용만 본다. */
    record StatsRow(LocalDateTime selectedAt, Integer estimatedCost) {
    }

    /**
     * 요약을 계산할 재료를 가져온다.
     *
     * <p>SQL 에서 COUNT · SUM 으로 끝내지 않는 이유는 <b>연속 방문일</b> 때문이다.
     * 며칠 연속인지는 한국 날짜로 묶어 세어야 하는데, DB 에는 UTC 가 들어 있어서
     * 오라클에서 날짜로 바꾸면 세션 시간대에 의존하게 된다. 행을 그대로 받아
     * 자바에서 세는 편이 파이썬 쪽과 같은 답을 낸다.
     */
    List<StatsRow> findStatsRows(@Param("userId") Long userId);

    /** 목록 한 줄 + JOIN 으로 계산한 공유 글 id. */
    record PageRow(MealHistory history, Long sharedPostId) {
    }

    /**
     * 조건에 맞는 전체 건수.
     *
     * <p>목록과 <b>같은 조건 · 같은 JOIN</b> 을 봐야 한다. 한쪽에만 필터를 붙이면
     * "3페이지가 있다는데 열면 비어 있는" 일이 생긴다.
     */
    int countByUser(@Param("userId") Long userId, @Param("shared") Boolean shared);

    /** 내 기록 한 페이지. 최신순. */
    List<PageRow> findPageByUser(
            @Param("userId") Long userId,
            @Param("shared") Boolean shared,
            @Param("offset") int offset,
            @Param("size") int size);

    /** id 로 하나. 남의 것인지 · 지웠는지 판단은 부르는 쪽이 한다. */
    MealHistory findById(@Param("id") Long id);

    /** 식사 확정. id · selected_at 은 DB 가 채운다. */
    void insert(MealHistory history);

    /** 삭제. 행을 지우지 않고 use_yn='N'. */
    void deactivate(@Param("id") Long id);

    /**
     * 이 기록으로 쓴 후기 id. 없으면 null.
     *
     * <p>기록을 지우려 할 때 확인한다. 공유한 기록을 지우면 후기가 근거를 잃는다.
     */
    Long findSharedReviewId(@Param("historyId") Long historyId);
}
