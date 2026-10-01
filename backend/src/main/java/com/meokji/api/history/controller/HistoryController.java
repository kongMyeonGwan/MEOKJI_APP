package com.meokji.api.history.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.common.Page;
import com.meokji.api.history.vo.MealHistoryStatsVO;
import com.meokji.api.history.vo.MealHistoryVO;
import com.meokji.api.auth.service.AuthService;
import com.meokji.api.history.service.HistoryService;
import com.meokji.api.user.vo.UserVO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 내 식사 기록 ("이걸로 먹을래요" 를 누른 레시피).  주소: /api/histories
 * 로그인이 필요하다.
 */
@RestController
@RequestMapping("/api/histories")
public class HistoryController {

    @Autowired
    private AuthService authService;

    @Autowired
    private HistoryService historyService;

    // 내 기록 한 페이지  GET /api/histories?page=1&size=20&shared=false
    //   shared : true 면 커뮤니티에 공유한 것만, false 면 공유 안 한 것만, 안 보내면 전부
    @GetMapping
    public Page<MealHistoryVO> getList(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(Page.MAX_SIZE) int size,
            @RequestParam(required = false) Boolean shared) {
        UserVO loginUser = authService.getLoginUser(token);
        return historyService.getList(loginUser, page, size, shared);
    }

    // 요약 (총 건수, 누적 재료비, 연속 기록일)  GET /api/histories/stats
    @GetMapping("/stats")
    public MealHistoryStatsVO getStats(@RequestHeader(value = "Authorization", required = false) String token) {
        UserVO loginUser = authService.getLoginUser(token);
        return historyService.getStats(loginUser);
    }

    // 기록 저장  POST /api/histories
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // 201
    public MealHistoryVO add(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody MealHistoryVO request) {
        UserVO loginUser = authService.getLoginUser(token);
        return historyService.add(loginUser, request);
    }

    // 기록 삭제  DELETE /api/histories/3
    @DeleteMapping("/{historyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void delete(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long historyId) {
        UserVO loginUser = authService.getLoginUser(token);
        historyService.delete(loginUser, historyId);
    }
}
