package com.meokji.api.recommend.service;

import java.util.List;

import com.meokji.api.recommend.dto.BargainItem;

/** 요즘 값이 내린 재료 ("지금 싼 재료로" 카드). 실제 코드는 BargainServiceImpl 에 있다. */
public interface BargainService {

    // 일주일 전보다 값이 5% 이상 내린 재료 (최대 8개, 많이 내린 순)
    List<BargainItem> current();
}
