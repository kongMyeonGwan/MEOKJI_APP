package com.meokji.api.history.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meokji.api.common.ApiException;
import com.meokji.api.common.Page;
import com.meokji.api.history.dao.HistoryDAO;
import com.meokji.api.history.dto.MealHistoryCreateRequest;
import com.meokji.api.history.dto.MealHistoryResponse;
import com.meokji.api.history.dto.MealHistoryStats;
import com.meokji.api.util.FoodCatalog;
import com.meokji.api.util.Json;
import com.meokji.api.util.Kst;
import com.meokji.api.util.Times;
import com.meokji.api.history.vo.MealHistoryVO;
import com.meokji.api.user.vo.UserVO;

@Service
public class HistoryServiceImpl implements HistoryService {

    @Autowired
    private HistoryDAO historyDAO;

    @Autowired
    private FoodCatalog foodCatalog;

    // ===================== 목록 =====================

    @Override
    public Page<MealHistoryResponse> getList(UserVO user, int page, int size, Boolean shared) {
        int total = historyDAO.countByUser(user.getId(), shared);

        int offset = (page - 1) * size; // 앞에서 몇 개를 건너뛸지 (2페이지면 20개)
        List<MealHistoryResponse> items = new ArrayList<>();
        for (MealHistoryVO history : historyDAO.findPageByUser(user.getId(), shared, offset, size)) {
            items.add(toResponse(history));
        }

        // 전체 페이지 수. 45개를 20개씩 나누면 3페이지. 0개면 0페이지.
        int totalPages = 0;
        if (size > 0) {
            totalPages = (total + size - 1) / size;
        }

        Page<MealHistoryResponse> result = new Page<>();
        result.setItems(items);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setTotalPages(totalPages);
        return result;
    }

    // ===================== 요약 =====================

    @Override
    public MealHistoryStats getStats(UserVO user) {
        List<MealHistoryVO> histories = historyDAO.findStatsRows(user.getId());

        long totalCost = 0;
        Set<LocalDate> days = new HashSet<>();
        for (MealHistoryVO history : histories) {
            if (history.getEstimatedCost() != null) {
                totalCost = totalCost + history.getEstimatedCost();
            }
            days.add(Kst.toDate(history.getSelectedAt()));
        }

        MealHistoryStats stats = new MealHistoryStats();
        stats.setCount(histories.size());
        stats.setTotalCost(totalCost);
        stats.setStreakDays(Kst.countStreak(days));
        return stats;
    }

    // ===================== 저장 =====================

    @Override
    @Transactional
    public MealHistoryResponse add(UserVO user, MealHistoryCreateRequest request) {
        MealHistoryVO history = new MealHistoryVO();
        history.setUserId(user.getId());
        history.setFoodName(request.getFoodName().trim());
        history.setRecipeName(request.getRecipeName());
        // 레시피 · 재료는 JSON 을 글자로 바꿔서 저장한다 (DB 컬럼이 CLOB 글자라서)
        history.setRecipeJson(Json.text(request.getRecipeJson()));
        history.setIngredientsJson(Json.text(request.getIngredientsJson()));
        history.setSubstitutesJson(Json.text(request.getSubstitutesJson()));
        history.setEstimatedCost(request.getEstimatedCost());

        historyDAO.insert(history);

        // 저장한 시각(DB 가 채움)까지 다시 읽어서 돌려준다
        return toResponse(historyDAO.findById(history.getId()));
    }

    // ===================== 삭제 =====================

    @Override
    @Transactional
    public void delete(UserVO user, Long historyId) {
        MealHistoryVO history = historyDAO.findById(historyId);

        // 없거나, 남의 것이거나, 이미 지운 것이면 404
        if (history == null || !history.getUserId().equals(user.getId()) || !"Y".equals(history.getUseYn())) {
            throw ApiException.notFound("기록을 찾을 수 없습니다.");
        }

        // 이 기록으로 커뮤니티에 글을 썼으면 못 지운다 (글이 레시피를 잃어버리니까)
        if (historyDAO.findSharedReviewId(historyId) != null) {
            throw ApiException.conflict("커뮤니티에 공유한 기록입니다. 글을 먼저 삭제해주세요.");
        }

        historyDAO.deactivate(historyId);
    }

    // MealHistoryVO(DB 한 행) → 응답 모양으로 옮겨 담는다
    private MealHistoryResponse toResponse(MealHistoryVO history) {
        MealHistoryResponse response = new MealHistoryResponse();
        response.setId(history.getId());
        response.setFoodName(history.getFoodName());
        response.setRecipeName(history.getRecipeName());
        // DB 에 글자로 저장된 JSON 을 다시 JSON 으로 바꾼다
        response.setRecipeJson(Json.node(history.getRecipeJson()));
        response.setIngredientsJson(Json.node(history.getIngredientsJson()));
        response.setSubstitutesJson(Json.node(history.getSubstitutesJson()));
        response.setEstimatedCost(history.getEstimatedCost());
        response.setSelectedAt(Times.toText(history.getSelectedAt()));
        response.setSharedPostId(history.getSharedPostId());
        // 썸네일 그림 종류. 음식 목록(catalog)에 같은 이름이 있으면 가져온다
        response.setArt(foodCatalog.findArt(history.getFoodName()));
        return response;
    }
}
