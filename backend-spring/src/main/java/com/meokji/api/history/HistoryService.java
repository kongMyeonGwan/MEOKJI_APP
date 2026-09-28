package com.meokji.api.history;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meokji.api.catalog.FoodCatalog;
import com.meokji.api.common.ApiException;
import com.meokji.api.common.Json;
import com.meokji.api.common.Kst;
import com.meokji.api.common.Page;
import com.meokji.api.common.Times;
import com.meokji.api.history.dto.MealHistoryCreateRequest;
import com.meokji.api.history.dto.MealHistoryResponse;
import com.meokji.api.history.dto.MealHistoryStats;
import com.meokji.api.user.User;

/**
 * 내 식사 기록 (STEP 15~16).
 *
 * <p>파이썬 쪽 {@code app/services/history_service.py} 자리다.
 */
@Service
public class HistoryService {

    @Autowired
    private HistoryMapper histories;
    @Autowired
    private FoodCatalog catalog;


    /**
     * 내 기록 한 페이지. 최신순.
     *
     * @param shared true=공유한 것만 · false=아직 안 한 것만 · null=전부
     */
    public Page<MealHistoryResponse> list(User user, int page, int size, Boolean shared) {
        int total = histories.countByUser(user.getId(), shared);

        List<MealHistoryResponse> items = new ArrayList<>();

        // 범위를 벗어난 페이지도 404 가 아니라 빈 items 다. 목록을 보다가 마지막
        // 글이 지워지면 열려 있던 페이지가 사라지는데, 그때 404 를 내면 화면이
        // 깨진 것처럼 보인다.
        for (HistoryMapper.PageRow row
                : histories.findPageByUser(user.getId(), shared, (page - 1) * size, size)) {
            items.add(toResponse(row.history(), row.sharedPostId()));
        }

        return Page.of(items, total, page, size);
    }

    /** 건수 · 누적 비용 · 연속 방문일. 전체 기준이다. */
    public MealHistoryStats stats(User user) {
        List<HistoryMapper.StatsRow> rows = histories.findStatsRows(user.getId());

        long totalCost = 0;
        Set<LocalDate> days = new HashSet<>();

        for (HistoryMapper.StatsRow row : rows) {
            // estimated_cost 가 NULL 인 기록도 있다. 0 으로 친다.
            if (row.estimatedCost() != null) {
                totalCost += row.estimatedCost();
            }
            days.add(Kst.toDate(row.selectedAt()));
        }

        return new MealHistoryStats(rows.size(), totalCost, countStreak(days));
    }

    /**
     * 오늘(또는 어제)부터 며칠 연속으로 기록이 있는지 센다.
     *
     * <p>{@code UserService} 에도 같은 계산이 있다. 한쪽으로 모으는 것이 맞지만,
     * 지금은 내 정보 화면과 히스토리 화면이 같은 값을 보여주는지 확인하는 용도로
     * 둘 다 둔다 - 옮기는 중에 한쪽만 틀리면 그게 드러난다.
     */
    static int countStreak(Set<LocalDate> days) {
        if (days.isEmpty()) {
            return 0;
        }

        LocalDate today = Kst.today();
        LocalDate cursor;

        if (days.contains(today)) {
            cursor = today;
        } else if (days.contains(today.minusDays(1))) {
            cursor = today.minusDays(1);
        } else {
            return 0;
        }

        int streak = 0;
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }

        return streak;
    }

    /** 식사 확정. 레시피와 재료를 그 시점 스냅샷으로 저장한다. */
    @Transactional
    public MealHistoryResponse create(User user, MealHistoryCreateRequest request) {
        MealHistory history = new MealHistory();
        history.setUserId(user.getId());
        history.setFoodName(request.trimmedFoodName());
        history.setRecipeName(request.recipeName());
        history.setRecipeJson(Json.text(request.recipeJson()));
        history.setIngredientsJson(Json.text(request.ingredientsJson()));
        history.setSubstitutesJson(Json.text(request.substitutesJson()));
        history.setEstimatedCost(request.estimatedCost());

        histories.insert(history);

        // DB 가 채운 selected_at 을 읽어온다. 방금 만든 기록이라 아직 공유했을 수 없다.
        return toResponse(histories.findById(history.getId()), null);
    }

    /** 내 기록 하나를 지운다 (use_yn='N'). */
    @Transactional
    public void delete(User user, Long historyId) {
        MealHistory history = findMine(user, historyId);

        Long sharedReviewId = histories.findSharedReviewId(history.getId());

        if (sharedReviewId != null) {
            // 기록을 지우면 후기가 근거를 잃는다. 후기는 자기 기록에서만 쓸 수
            // 있다는 규칙이 FK 로 걸려 있어서, 순서를 바꿔 지워야 한다.
            throw ApiException.conflict("커뮤니티에 공유한 기록입니다. 글을 먼저 삭제해주세요.");
        }

        histories.deactivate(historyId);
    }

    /**
     * 내 기록 하나를 가져온다.
     *
     * <p>없는 id · 남의 기록 · 이미 지운 기록을 <b>모두 같은 404</b> 로 답한다.
     * 남의 기록에 403 을 주면 "그 id 는 있다" 는 사실이 새어나간다.
     */
    private MealHistory findMine(User user, Long historyId) {
        MealHistory history = histories.findById(historyId);

        if (history == null
                || !history.getUserId().equals(user.getId())
                || !"Y".equals(history.getUseYn())) {
            throw ApiException.notFound("기록을 찾을 수 없습니다.");
        }

        return history;
    }

    /** 표의 한 행을 응답으로. DB 에 없는 값(공유 글 id · 썸네일)을 붙인다. */
    private MealHistoryResponse toResponse(MealHistory history, Long sharedPostId) {
        return new MealHistoryResponse(
                history.getId(),
                history.getFoodName(),
                history.getRecipeName(),
                Json.node(history.getRecipeJson()),
                Json.node(history.getIngredientsJson()),
                Json.node(history.getSubstitutesJson()),
                history.getEstimatedCost(),
                Times.toText(history.getSelectedAt()),
                sharedPostId,
                // 썸네일은 DB 에 없다. 카탈로그에 같은 이름이 있으면 가져다 쓴다.
                // 사용자가 직접 검색한 이름이면 없는 것이 정상이다.
                catalog.findArt(history.getFoodName()));
    }
}
