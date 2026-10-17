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

/**
 * 식사 기록 목록 · 요약 · 저장 · 삭제.
 *
 * <p>이 기능만의 사정이 둘 있다
 *
 * <p>1. 응답에 art(썸네일 그림 이름)를 끼워 넣는다. MEAL_HISTORIES 에 없는
 * 칸이라 catalog.json 에서 음식 이름으로 찾아야 한다. 표에 같이 저장하지
 * 않는 이유는, 그림을 바꾸면 과거 기록까지 다 UPDATE 해야 하기 때문이다.
 *
 * <p>2. 연속 기록일을 자바에서 센다. SQL 로도 되지만(분석 함수) 읽기 어려운
 * 쿼리가 되고, "날짜가 하루씩 이어지는지" 는 자바로 훑는 게 눈에 보인다.
 */
@Service
public class HistoryServiceImpl implements HistoryService {

    @Autowired
    private HistoryDAO historyDAO;

    // 음식 이름 -> 썸네일 그림 이름. art 를 채우는 데만 쓴다
    @Autowired
    private FoodCatalog foodCatalog;

    // ===================== 목록 =====================

    /**
     * 내 기록 한 페이지.
     *
     * {@code @Transactional} 이 없다. 읽기만 한다.
     *
     * SQL 을 두 번 보낸다 — 전체 건수(count)와 이 페이지(page). 한 번에
     * 할 수도 있지만, 건수를 알아야 전체 페이지 수를 계산할 수 있다.
     */
    @Override
    public Page<MealHistoryVO> getList(UserVO user, int page, int size, Boolean shared) {
        // offset 과 total_pages 계산은 Paging 이 한다 (common/Paging.java)
        //
        // 여기서 (page - 1) * size 를 직접 계산하지 않는 이유 — 목록을 주는
        // 서비스가 넷(history · notice · review · free)이고, 각자 계산하면
        // 한 곳에서 실수해도 에러가 안 난다. 조용히 엉뚱한 페이지가 나온다
        Paging paging = new Paging(page, size);
        // 전체 건수를 알려줘야 Paging 이 total_pages 를 계산할 수 있다.
        // shared 조건을 목록과 똑같이 넘긴다 - 한쪽에만 걸면 건수와 목록이 어긋난다
        paging.setTotal(historyDAO.countByUser(user.getId(), shared));

        // 받아온 행마다 art 를 채워 넣는다.
        //
        // for 로 하나씩 도는 이유 — art 는 음식 이름으로 catalog.json 을
        // 찾아야 나오는 값이라 SQL 로는 채울 수 없다. 20건이라 부담은 없다
        List<MealHistoryVO> items = new ArrayList<>();
        for (MealHistoryVO history : historyDAO.findPageByUser(user.getId(), shared, paging.getOffset(),
                paging.getSize())) {
            items.add(withArt(history));
        }

        Page<MealHistoryVO> result = new Page<>();
<<<<<<< HEAD
        // total · page · size · total_pages · items 를 한 번에 채운다.
        // setter 다섯 개를 늘어놓으면 한 칸을 빠뜨려도 컴파일이 된다
        result.fill(paging, items);
=======
        result.Page(paging, items);
>>>>>>> 137ab2e5d83abad60912fb1f0b6b8a1315e728eb
        return result;
    }

    // ===================== 요약 =====================

    /**
     * 총 건수 · 누적 재료비 · 연속 기록일.
     *
     * COUNT · SUM 을 SQL 로 바로 구하지 않고 행을 전부 받아와 세는 이유 —
     * 연속 기록일을 같은 자료로 계산해야 한다. SQL 로 세 값을 각각 구하면
     * DB 를 세 번 다녀오고, 그 사이에 기록이 하나 더 들어오면 숫자가 서로
     * 안 맞는다(건수는 13인데 재료비는 12건 합계).
     */
    @Override
    public MealHistoryStatsVO getStats(UserVO user) {
        List<MealHistoryVO> histories = historyDAO.findStatsRows(user.getId());

        long totalCost = 0;
        // 날짜를 Set 에 담는 이유 — 하루에 두 끼를 기록해도 "기록한 날" 은
        // 하루다. 목록이면 둘 다 보여야 하지만, 연속일을 셀 때는 한 번만 센다
        Set<LocalDate> days = new HashSet<>();
        for (MealHistoryVO history : histories) {
            // 재료비를 계산할 수 없었던 기록은 null 이다. 0 으로 보고 넘어간다 -
            // null 을 더하려 하면 NullPointerException 이 난다
            if (history.getEstimatedCost() != null) {
                totalCost = totalCost + history.getEstimatedCost();
            }
            // DB 에는 UTC 로 쌓여 있다. 그대로 날짜만 떼면 밤 9시 이후의
            // 기록이 다음 날로 넘어간다(UTC + 9 = KST). 한국 날짜로 바꿔 센다
            days.add(Kst.toDate(history.getSelectedAt()));
        }

        MealHistoryStatsVO stats = new MealHistoryStatsVO();
        // 건수는 날짜가 아니라 기록 수다. 하루에 두 끼면 2 로 센다
        stats.setCount(histories.size());
        stats.setTotalCost(totalCost);
        // 오늘부터 거꾸로 하루씩 이어지는 동안의 일수. 계산은 Kst 가 한다
        stats.setStreakDays(Kst.countStreak(days));
        return stats;
    }

    // ===================== 저장 =====================

    /**
     * 기록 하나 저장.
     *
     * {@code @Transactional} 이 붙은 이유 — INSERT 뒤에 findById 로 다시
     * 읽는다. 둘이 한 트랜잭션 안에 있어야 방금 넣은 행을 확실히 읽는다.
     */
    @Override
    @Transactional
    public MealHistoryVO add(UserVO user, MealHistoryVO request) {
        // 화면이 보낸 값이 이미 MealHistoryVO 에 담겨 있다.
        // JSON 세 칸도 setter 가 글자로 바꿔 넣어줬다 (DB 컬럼이 CLOB 글자라서).
        //
        // 받은 VO 를 그대로 insert 에 넘기지 않고 새로 만들어 옮겨 담는다.
        // 화면이 id · useYn · selectedAt 을 섞어 보내도 들어가지 않게 하려는 것이다
        MealHistoryVO history = new MealHistoryVO();
        // 회원 번호는 화면이 보낸 값이 아니라 토큰에서 온 값을 쓴다
        history.setUserId(user.getId());
        // 앞뒤 공백을 떼어낸다. art 를 찾을 때 이름으로 맞춰보기 때문에
        // " 김치찌개" 로 들어가면 그림을 못 찾는다
        history.setFoodName(request.getFoodName().trim());
        history.setRecipeName(request.getRecipeName());
        // 아래 셋은 이미 글자다. getRecipeJson() 은 롬복이 만든 보통 getter 라
        // JsonNode 로 바꾸지 않고 저장된 글자를 그대로 꺼낸다
        history.setRecipeJson(request.getRecipeJson());
        history.setIngredientsJson(request.getIngredientsJson());
        history.setSubstitutesJson(request.getSubstitutesJson());
        history.setEstimatedCost(request.getEstimatedCost());

        historyDAO.insert(history);

        // 저장한 시각(DB 가 채움)까지 다시 읽어서 돌려준다
        //
        // 다시 읽는 이유가 둘이다 — selected_at 은 DDL 의 DEFAULT 가 찍고,
        // recipe_review_id 는 JOIN 으로 따라오는 칸이라 insert 로는 안 채워진다
        return withArt(historyDAO.findById(history.getId()));
    }

    // ===================== 삭제 =====================

    /**
     * 기록 하나 삭제 (use_yn = 'N').
     *
     * {@code @Transactional} 이 붙은 이유 — SELECT 두 번과 UPDATE 하나가
     * 한 묶음이다. 확인과 변경 사이에 상태가 바뀌면 안 된다.
     */
    @Override
    @Transactional
    public void delete(UserVO user, Long historyId) {
        MealHistoryVO history = historyDAO.findById(historyId);

        // 없거나, 남의 것이거나, 이미 지운 것이면 404
        //
        // 남의 것일 때 403 이 아니라 404 인 이유 — 403 을 주면 id 를 하나씩
        // 넣어보면서 "그 번호의 기록이 있는지" 를 알아낼 수 있다
        if (history == null || !history.getUserId().equals(user.getId()) || !"Y".equals(history.getUseYn())) {
            throw ApiException.notFound("기록을 찾을 수 없습니다.");
        }

        // 이 기록으로 커뮤니티에 글을 썼으면 못 지운다 (글이 레시피를 잃어버리니까)
        //
        // 404 가 아니라 409 를 주는 이유 — 기록은 분명히 있고 내 것도 맞다.
        // "지금 상태와 부딪친다" 는 뜻이라 문장도 어떻게 해야 하는지 알려준다
        if (historyDAO.findSharedReviewId(historyId) != null) {
            throw ApiException.conflict("커뮤니티에 공유한 기록입니다. 글을 먼저 삭제해주세요.");
        }

        historyDAO.deactivate(historyId);
    }

    // MealHistoryVO(DB 한 행) → 응답 모양으로 옮겨 담는다
    // 썸네일 그림 종류를 채워서 돌려준다.
    // art 는 표에 없는 칸이라 음식 목록(catalog)에서 찾아 넣어야 한다.
    //
    // 받은 객체를 고쳐서 그대로 돌려주는 모양이다. 새 객체를 만들어 칸을
    // 전부 옮겨 담으면 칸이 열 개가 넘어 길어지고, 칸을 하나 늘릴 때
    // 여기도 같이 고쳐야 한다.
    //
    // catalog 에 없는 음식이면 findArt 가 null 을 준다. 사용자가 직접 검색한
    // 이름으로도 기록이 남을 수 있어서 정상으로 본다 - 화면은 null 이면
    // 기본 그림을 쓴다.
    private MealHistoryVO withArt(MealHistoryVO history) {
        history.setArt(foodCatalog.findArt(history.getFoodName()));
        return history;
    }
}
