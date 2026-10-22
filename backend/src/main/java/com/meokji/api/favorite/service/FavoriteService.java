package com.meokji.api.favorite.service;

import com.meokji.api.favorite.dto.FavoriteListResponse;
import com.meokji.api.favorite.dto.FavoritesResponse;
import com.meokji.api.favorite.repository.FavoriteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** 찜한 레시피 */
@Service
@Transactional(readOnly = true)
public class FavoriteService {

    @Autowired
    private FavoriteRepository favoriteRepository;

    public FavoritesResponse getList(Long userId) {
        // 1. DB에서 찌 목록 (최신순)
        List<FavoriteListResponse> items = favoriteRepository.findList(userId);

        // 2. 음식 이름만 모은다 (하트 표시용)
        List<String> names = new ArrayList<>();
        for(FavoriteListResponse item : items){
            names.add(item.getFoodName());
        }

        // 3. 개수
        int count = items.size();

        return new FavoritesResponse(names, items, count);
    }

}
