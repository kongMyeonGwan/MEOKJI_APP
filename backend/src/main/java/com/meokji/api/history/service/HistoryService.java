package com.meokji.api.history.service;

import com.meokji.api.common.Page;
import com.meokji.api.history.vo.MealHistoryStatsVO;
import com.meokji.api.history.vo.MealHistoryVO;
import com.meokji.api.user.vo.UserVO;

/** 내 식사 기록. 실제 코드는 HistoryServiceImpl 에 있다. */
public interface HistoryService {

    // 내 기록 한 페이지 (최신순)
    Page<MealHistoryVO> getList(UserVO user, int page, int size, Boolean shared);

    // 요약 (총 건수, 누적 재료비, 연속 기록일)
    MealHistoryStatsVO getStats(UserVO user);

    // 기록 저장
    MealHistoryVO add(UserVO user, MealHistoryVO request);

    // 기록 삭제. 커뮤니티에 공유한 기록이면 409 에러
    void delete(UserVO user, Long historyId);
}
