package com.meokji.api.history.service;

import com.meokji.api.common.Page;
import com.meokji.api.history.vo.MealHistoryStatsVO;
import com.meokji.api.history.vo.MealHistoryVO;
import com.meokji.api.user.vo.UserVO;

/**
 * 내 식사 기록. 실제 코드는 HistoryServiceImpl 에 있다.
 *
 * <p>네 메서드 모두 첫 인자가 UserVO 다. 기록은 전부 "내 것" 이라서
 * 누구인지가 모든 판단에 들어간다.
 */
public interface HistoryService {

    /**
     * 내 기록 한 페이지 (최신순)
     *
     * @param shared true 면 공유한 것만, false 면 공유 안 한 것만, null 이면 전부
     * @return 목록과 함께 전체 건수 · 전체 페이지 수가 들어 있다
     */
    Page<MealHistoryVO> getList(UserVO user, int page, int size, Boolean shared);

    /**
     * 요약 (총 건수, 누적 재료비, 연속 기록일)
     *
     * 마이페이지 카드가 쓴다. 목록을 받아 화면에서 세지 않는 이유는
     * 화면이 한 페이지(20건)만 가지고 있어서 전체를 셀 수 없기 때문이다.
     */
    MealHistoryStatsVO getStats(UserVO user);

    /**
     * 기록 저장
     *
     * @return 새로 만든 행. id 와 selected_at 이 채워진 상태로 돌아온다
     */
    MealHistoryVO add(UserVO user, MealHistoryVO request);

    /**
     * 기록 삭제. 커뮤니티에 공유한 기록이면 409 에러
     *
     * 공유한 기록을 못 지우게 막는 이유 — 후기는 "실제로 먹어봤다" 는
     * 근거로 이 기록을 가리킨다. 기록이 사라지면 그 근거가 없어진다.
     * 후기를 먼저 지우면 이 기록도 지울 수 있다.
     */
    void delete(UserVO user, Long historyId);
}
