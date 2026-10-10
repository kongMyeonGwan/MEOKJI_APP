package com.meokji.api.history.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.meokji.api.history.vo.MealHistoryVO;

/**
 * MEAL_HISTORIES 표에 보내는 SQL.
 *
 * <p>이 표는 "이걸로 먹을래요" 를 누른 기록이다. 세는 데도 쓰이고(요약),
 * 목록으로도 나가고, 후기의 근거도 된다 - 쓰는 데가 많아서 메서드가 일곱 개다.
 *
 * <p>요약값(findStatsRows)이 가장 먼저 생겼다. 내 정보 화면이 연속 기록일과
 * 요리 횟수를 같이 보여주기 때문에, 목록 화면보다 먼저 필요했다.
 *
 * <p>실제 SQL 은 resources/mapper/HistoryMapper.xml 에 있다.
 */
@Mapper
public interface HistoryDAO {

    /**
     * 요약을 계산할 재료를 가져온다.
     *
     * COUNT · SUM 을 SQL 로 바로 구하지 않고 행을 받아오는 이유 — 연속
     * 기록일(streak)은 "날짜가 하루씩 이어지는지" 를 날짜 순서대로 훑어야
     * 알 수 있다. SQL 로도 되지만(분석 함수) 읽기 어려운 쿼리가 된다.
     *
     * 기록이 많아지면 전부 읽어오는 것이 부담이 될 수 있는 모양이다.
     * 지금은 한 사람의 기록이 수백 건을 넘지 않아 문제가 없다.
     */
    List<MealHistoryVO> findStatsRows(@Param("userId") Long userId);

    /**
     * 조건에 맞는 전체 건수.
     *
     * 목록과 같은 조건 · 같은 JOIN 을 봐야 한다. 한쪽에만 필터를 붙이면
     * "3페이지가 있다는데 열면 비어 있는" 일이 생긴다.
     *
     * @param shared true 면 공유한 것만, false 면 공유 안 한 것만,
     *               null 이면 전부. XML 의 if 가 null 을 보고 조건을 뺀다
     */
    int countByUser(@Param("userId") Long userId, @Param("shared") Boolean shared);

    /**
     * 내 기록 한 페이지. 최신순.
     *
     * offset · size 를 받아 SQL 이 그만큼만 잘라 온다. 전부 읽어 자바에서
     * 자르면 1000건 중 20건을 보려고 1000건을 서버 메모리에 올린다.
     *
     * 최신순으로 고정한 이유 — 정렬을 정하지 않으면 페이지를 넘길 때마다
     * 순서가 달라져 같은 기록이 두 페이지에 나오거나 아예 안 나온다.
     */
    List<MealHistoryVO> findPageByUser(
            @Param("userId") Long userId,
            @Param("shared") Boolean shared,
            @Param("offset") int offset,
            @Param("size") int size);

    /**
     * id 로 하나. 남의 것인지 · 지웠는지 판단은 부르는 쪽이 한다.
     *
     * SQL 에 user_id 조건을 넣지 않은 이유 — 넣으면 "없는 번호" 와 "남의 것"
     * 이 똑같이 null 로 돌아와서 서비스가 둘을 구별할 수 없다.
     */
    MealHistoryVO findById(@Param("id") Long id);

    /** 식사 확정. id · selected_at 은 DB 가 채운다. */
    void insert(MealHistoryVO history);

    /**
     * 삭제. 행을 지우지 않고 use_yn='N'.
     *
     * 행째로 지우면 이 기록을 근거로 쓴 후기가 가리킬 곳을 잃는다.
     * 요약값(총 건수 · 누적 재료비)도 과거까지 같이 바뀐다.
     */
    void deactivate(@Param("id") Long id);

    /**
     * 이 기록으로 쓴 후기 id. 없으면 null.
     *
     * 기록을 지우려 할 때 확인한다. 공유한 기록을 지우면 후기가 근거를 잃는다.
     *
     * 후기 전체가 아니라 id 만 받는 이유 — 있는지 없는지만 보면 된다.
     * 내용 · 사진까지 읽어올 이유가 없다.
     */
    Long findSharedReviewId(@Param("historyId") Long historyId);
}
