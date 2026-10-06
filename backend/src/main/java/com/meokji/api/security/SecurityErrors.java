package com.meokji.api.security;

import java.io.IOException;

import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 보안 필터에서 난 에러를 응답으로 쓴다.
 *
 * <h3>왜 ApiExceptionHandler 를 못 쓰나</h3>
 *
 * 평소 서비스에서 {@code throw ApiException.unauthorized(...)} 를 던지면
 * {@link com.meokji.api.common.ApiExceptionHandler} 가 받아서
 * {@code {"detail": "문장"}} 으로 바꿔준다.
 *
 * 그런데 그 핸들러는 <b>Controller 안에서 난 에러만</b> 받는다. 필터는 Controller 보다
 * 앞에서 돌기 때문에, 필터에서 던진 예외는 핸들러까지 가지 않고 톰캣의 기본 에러 화면이 나간다.
 *
 * <pre>
 *   요청 → [보안 필터] → DispatcherServlet → Controller → Service
 *            ↑                                  ↑
 *            여기서 난 에러는                    여기서 난 에러만
 *            핸들러가 못 잡는다                  ApiExceptionHandler 가 잡는다
 * </pre>
 *
 * 그래서 응답 모양을 맞추려면 여기서 직접 적어야 한다. 프론트는 어디서 난 에러든
 * {@code detail} 칸만 읽으므로, 모양이 달라지면 화면에 에러 문구가 안 뜬다.
 *
 * <h3>실패 사유를 request 에 적어두는 이유</h3>
 *
 * 401 이라도 사정이 다르다 - "토큰을 아예 안 보냈다" 와 "보냈는데 만료됐다" 는
 * 프론트가 다르게 처리해야 한다(후자는 재발급을 시도할 수 있다).
 *
 * 그런데 필터는 그 자리에서 401 을 쓸 수가 없다. 로그인 없이도 되는 주소(레시피,
 * 게시판 목록)에 썩은 토큰이 딸려 와도 그냥 손님으로 보여줘야 하기 때문이다.
 * 그래서 필터는 "왜 인증을 못 했는지" 만 request 에 적어두고 그대로 통과시키고,
 * 로그인이 필요한 주소였다면 그제서야 {@link JwtAuthenticationEntryPoint} 가
 * 그 메모를 읽어 401 을 쓴다.
 */
final class SecurityErrors {

    /** 필터가 실패 사유를 적어두는 자리 이름. EntryPoint 가 이 이름으로 꺼내 읽는다. */
    static final String ERROR_MESSAGE = "meokji.auth.errorMessage";

    /** 상태 코드까지 달라지는 경우에만 적는다 (계정 잠김 = 423). 없으면 401. */
    static final String ERROR_STATUS = "meokji.auth.errorStatus";

    private SecurityErrors() {
    }

    /** 필터가 실패 사유를 적어둔다. 상태 코드는 401 이다. */
    static void remember(HttpServletRequest request, String message) {
        request.setAttribute(ERROR_MESSAGE, message);
    }

    /** 필터가 실패 사유를 적어둔다. 상태 코드까지 지정한다. */
    static void remember(HttpServletRequest request, int status, String message) {
        request.setAttribute(ERROR_MESSAGE, message);
        request.setAttribute(ERROR_STATUS, status);
    }

    /** 적어둔 사유를 꺼낸다. 없으면 null. */
    static String rememberedMessage(HttpServletRequest request) {
        return (String) request.getAttribute(ERROR_MESSAGE);
    }

    /** 적어둔 상태 코드를 꺼낸다. 없으면 기본값을 그대로 쓴다. */
    static int rememberedStatus(HttpServletRequest request, int fallback) {
        Object status = request.getAttribute(ERROR_STATUS);
        if (status == null) {
            return fallback;
        }
        return (Integer) status;
    }

    /**
     * {@code {"detail": "문장"}} 을 응답에 쓴다.
     *
     * 문구에 한글이 있으므로 UTF-8 을 반드시 지정한다. 빼면 화면에 글자가 깨져 나온다.
     */
    static void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // 직접 적는다. 문장에 큰따옴표가 들어갈 일이 없는 고정 문구들이라
        // Jackson 을 끌어오지 않아도 된다. (아래 escape 로 한 번 더 막아둔다)
        response.getWriter().write("{\"detail\":\"" + escape(message) + "\"}");
    }

    /** JSON 이 깨지지 않게 큰따옴표와 역슬래시만 막아둔다. */
    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
