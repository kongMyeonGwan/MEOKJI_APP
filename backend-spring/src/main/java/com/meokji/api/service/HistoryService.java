package com.meokji.api.service;

import com.meokji.api.common.Page;
import com.meokji.api.dto.history.MealHistoryCreateRequest;
import com.meokji.api.dto.history.MealHistoryResponse;
import com.meokji.api.dto.history.MealHistoryStats;
import com.meokji.api.vo.UserVO;

/** 내 식사 기록. 실제 코드는 HistoryServiceImpl 에 있다. */
public interface HistoryService {

    // 내 기록 한 페이지 (최신순)
    Page<MealHistoryResponse> getList(UserVO user, int page, int size, Boolean shared);

    // 요약 (총 건수, 누적 재료비, 연속 기록일)
    MealHistoryStats getStats(UserVO user);

    // 기록 저장
    MealHistoryResponse add(UserVO user, MealHistoryCreateRequest request);

    // 기록 삭제. 커뮤니티에 공유한 기록이면 409 에러
    void delete(UserVO user, Long historyId);
}
