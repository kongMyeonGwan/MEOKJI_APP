package com.meokji.api.history;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.common.Page;
import com.meokji.api.history.dto.MealHistoryCreateRequest;
import com.meokji.api.history.dto.MealHistoryResponse;
import com.meokji.api.history.dto.MealHistoryStats;
import com.meokji.api.security.LoginUser;
import com.meokji.api.user.User;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 내 식사 기록 (STEP 15~16 · 페이징은 STEP 31).
 *
 * <p>파이썬 쪽 {@code app/routers/histories.py} 자리다.
 */
@RestController
@RequestMapping("/api/histories")
public class HistoryController {

    @Autowired
    private HistoryService historyService;


    /** 최신순 목록. 범위를 벗어난 페이지는 404 가 아니라 빈 목록이다. */
    @GetMapping
    public Page<MealHistoryResponse> list(
            @LoginUser User me,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(Page.MAX_SIZE) int size,
            @RequestParam(required = false) Boolean shared) {

        return historyService.list(me, page, size, shared);
    }

    /**
     * 요약. 건수 · 누적 비용 · 연속 방문일.
     *
     * <p><b>이 메서드가 {@code /{historyId}} 보다 위에 있어야 한다.</b> 아래에 두면
     * "stats" 를 id 로 읽으려다 실패한다.
     */
    @GetMapping("/stats")
    public MealHistoryStats stats(@LoginUser User me) {
        return historyService.stats(me);
    }

    /** 식사 확정. 레시피를 그 시점 스냅샷으로 저장한다. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MealHistoryResponse create(
            @LoginUser User me,
            @Valid @RequestBody MealHistoryCreateRequest request) {

        return historyService.create(me, request);
    }

    /** 삭제. 커뮤니티에 공유한 기록이면 409. */
    @DeleteMapping("/{historyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@LoginUser User me, @PathVariable @Min(1) Long historyId) {
        historyService.delete(me, historyId);
    }
}
