package com.meokji.api.recommend.service;

import java.util.List;

import com.meokji.api.recommend.vo.BargainVO;

/**
 * 요즘 값이 내린 재료 ("지금 싼 재료로" 카드). 실제 코드는 BargainServiceImpl 에 있다.
 *
 * <p>인자가 없다. 사람마다 다른 값이 아니라 "오늘 시세" 라서, 누가 물어도
 * 같은 답이 나온다. 그래서 로그인도 필요 없다.
 */
public interface BargainService {

    /**
     * 일주일 전보다 값이 5% 이상 내린 재료 (최대 8개, 많이 내린 순)
     *
     * 5% 와 8개는 보여줄 만한 선을 정한 값이다 — 1% 내린 것까지 담으면
     * "싸다" 고 할 수 없고, 20개를 담으면 카드가 화면을 덮는다.
     *
     * @return 조건에 맞는 재료가 없으면 빈 목록. null 이 아니다
     */
    List<BargainVO> current();
}
