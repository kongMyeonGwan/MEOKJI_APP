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
import com.meokji.api.common.Paging;
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
 *
 * <p>수정이 없다. 기록은 "그때 이걸 먹었다" 는 사실이라 고칠 대상이 아니다.
 * 잘못 눌렀으면 지우고 다시 넣는다.
 *
 * <p>이 기록이 세 곳에서 쓰인다
 *   - 마이페이지의 요약 카드 (총 건수 · 누적 재료비 · 연속 기록일)
 *   - 내 기록 목록 화면
 *   - 후기(review)의 근거 — 먹어본 사람만 후기를 쓸 수 있게 하려는 것이다
 *
 * <p>세 번째 때문에 삭제가 단순하지 않다. 후기의 근거가 된 기록은 지울 수
 * 없고, 서비스가 409 를 던진다.
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
    //
    // 기록은 계속 쌓이므로 페이징이 있다. ban(못 먹는 재료)과 달리
    // 한 번에 다 보낼 수 없다.
    //
    // @Min · @Max 를 컨트롤러에 거는 이유 — 음수 page 가 들어오면 SQL 의
    // OFFSET 이 음수가 되어 에러가 나고, size=10000 이면 한 번에 만 건을
    // 읽어 온다. 모양이 틀린 값은 서비스까지 들어오기 전에 막는다.
    //
    // Paging.MAX_SIZE 를 쓰는 이유 — 50 을 손으로 적으면 Paging 의 한계와
    // 어긋날 수 있다. 어긋나면 size=60 이 여기서는 통과하고 Paging 이
    // 조용히 50 으로 줄여서, 화면은 60개를 기대하는데 50개만 온다.
    //
    // shared 가 boolean 이 아니라 Boolean 인 이유 — "안 보냄(null)" 을
    // 구별해야 한다. boolean 이면 안 보낸 것이 false 가 되어
    // "공유 안 한 것만" 으로 바뀐다.
    @GetMapping
    public Page<MealHistoryVO> getList(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(Paging.MAX_SIZE) int size,
            @RequestParam(required = false) Boolean shared) {
        UserVO loginUser = authService.getLoginUser(token);
        return historyService.getList(loginUser, page, size, shared);
    }

    // 요약 (총 건수, 누적 재료비, 연속 기록일)  GET /api/histories/stats
    //
    // 목록과 주소를 나눈 이유 — 마이페이지는 요약만 쓰고 목록은 안 쓴다.
    // 같이 주면 요약 카드 세 개를 그리려고 기록 20건을 같이 받아간다.
    @GetMapping("/stats")
    public MealHistoryStatsVO getStats(@RequestHeader(value = "Authorization", required = false) String token) {
        UserVO loginUser = authService.getLoginUser(token);
        return historyService.getStats(loginUser);
    }

    // 기록 저장  POST /api/histories
    //
    // 받는 것과 돌려주는 것이 같은 MealHistoryVO 다. 받을 때 쓰는 칸과
    // 돌려줄 때 채워지는 칸이 다르다 - VO 쪽 주석에 적어 두었다
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // 201
    public MealHistoryVO add(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody MealHistoryVO request) {
        UserVO loginUser = authService.getLoginUser(token);
        return historyService.add(loginUser, request);
    }

    // 기록 삭제  DELETE /api/histories/3
    //
    // @Min(1) 을 건 이유 — id 는 시퀀스가 1부터 주는 값이라 0 이나 음수는
    // 있을 수 없다. 막지 않으면 서비스가 DB 를 한 번 다녀와서 404 를 낸다.
    //
    // 커뮤니티에 공유한 기록이면 서비스가 409 를 던진다 (204 가 안 나간다)
    @DeleteMapping("/{historyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void delete(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long historyId) {
        UserVO loginUser = authService.getLoginUser(token);
        historyService.delete(loginUser, historyId);
    }
}
