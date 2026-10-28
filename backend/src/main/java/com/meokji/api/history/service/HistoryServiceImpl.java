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
import com.meokji.api.common.Paging;
import com.meokji.api.history.dao.HistoryDAO;
import com.meokji.api.history.vo.MealHistoryStatsVO;
import com.meokji.api.util.FoodCatalog;
import com.meokji.api.util.Kst;
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
    public Page<MealHistoryVO> getList(UserVO user, int page, int size, Boolean shared) {
        // offset 과 total_pages 계산은 Paging 이 한다 (common/Paging.java)
        Paging paging = new Paging(page, size);
        paging.setTotal(historyDAO.countByUser(user.getId(), shared));

        List<MealHistoryVO> items = new ArrayList<>();
        for (MealHistoryVO history : historyDAO.findPageByUser(user.getId(), shared, paging.getOffset(),
                paging.getSize())) {
            items.add(withArt(history));
        }

        Page<MealHistoryVO> result = new Page<>();
        result.Page(paging, items);
        return result;
    }

    // ===================== 요약 =====================

    @Override
    public MealHistoryStatsVO getStats(UserVO user) {
        List<MealHistoryVO> histories = historyDAO.findStatsRows(user.getId());

        long totalCost = 0;
        Set<LocalDate> days = new HashSet<>();
        for (MealHistoryVO history : histories) {
            if (history.getEstimatedCost() != null) {
                totalCost = totalCost + history.getEstimatedCost();
            }
            days.add(Kst.toDate(history.getSelectedAt()));
        }

        MealHistoryStatsVO stats = new MealHistoryStatsVO();
        stats.setCount(histories.size());
        stats.setTotalCost(totalCost);
        stats.setStreakDays(Kst.countStreak(days));
        return stats;
    }

    // ===================== 저장 =====================

    @Override
    @Transactional
    public MealHistoryVO add(UserVO user, MealHistoryVO request) {
        String foodName = request.getFoodName().trim();

        // 같은 요리가 이미 기록에 있으면 또 넣지 않는다 (409).
        // 단, 그 기록으로 레시피 추천글을 썼으면 세지 않아서 새로 저장된다
        if (historyDAO.countUnsharedByFood(user.getId(), foodName) > 0) {
            throw ApiException.conflict(
                    "이미 History에 저장한 요리입니다. 이 기록으로 레시피 추천글을 쓰면 다시 저장할 수 있어요.");
        }

        // 화면이 보낸 값이 이미 MealHistoryVO 에 담겨 있다.
        // JSON 세 칸도 setter 가 글자로 바꿔 넣어줬다 (DB 컬럼이 CLOB 글자라서).
        MealHistoryVO history = new MealHistoryVO();
        history.setUserId(user.getId());
        history.setFoodName(foodName);
        history.setRecipeName(request.getRecipeName());
        history.setRecipeJson(request.getRecipeJson());
        history.setIngredientsJson(request.getIngredientsJson());
        history.setSubstitutesJson(request.getSubstitutesJson());
        history.setEstimatedCost(request.getEstimatedCost());

        historyDAO.insert(history);

        // 저장한 시각(DB 가 채움)까지 다시 읽어서 돌려준다
        return withArt(historyDAO.findById(history.getId()));
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
    // 썸네일 그림 종류를 채워서 돌려준다.
    // art 는 표에 없는 칸이라 음식 목록(catalog)에서 찾아 넣어야 한다.
    private MealHistoryVO withArt(MealHistoryVO history) {
        history.setArt(foodCatalog.findArt(history.getFoodName()));
        return history;
    }
}
