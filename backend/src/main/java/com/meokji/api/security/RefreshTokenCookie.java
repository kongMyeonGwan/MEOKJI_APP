package com.meokji.api.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.meokji.api.util.JwtUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Refresh Token 을 담는 쿠키를 만들고 읽는다.
 *
 * <h3>왜 쿠키인가 (Access Token 처럼 JSON 으로 주면 안 되나)</h3>
 *
 * Access Token 은 30분짜리라 새어도 피해가 제한적이지만, Refresh Token 은 14일짜리다.
 * 자바스크립트가 들고 있다가 XSS 한 번 당하면 2주 동안 남의 계정이 열린다.
 *
 * HttpOnly 쿠키는 자바스크립트가 <b>읽을 수조차 없다</b>. 브라우저만 알고, 요청을 보낼 때
 * 알아서 붙여준다. 우리 코드가 그 값을 만질 일이 아예 없으니 흘릴 자리도 없다.
 *
 * <pre>
 *   Access Token   JSON 으로 받아 React 메모리에 둔다 → 헤더에 직접 붙여 보낸다
 *   Refresh Token  HttpOnly 쿠키 → 브라우저가 알아서 붙인다, JS 는 못 본다
 * </pre>
 *
 * <h3>쿠키에 붙는 설정들</h3>
 *
 * <pre>
 *   HttpOnly  자바스크립트가 못 읽는다. 이게 이 방식의 핵심이다.
 *   Secure    https 로만 보낸다. 운영에서 반드시 켜야 한다.
 *   SameSite  남의 사이트가 보낸 요청에 이 쿠키를 붙일지. CSRF 방어다.
 *   Path      이 주소로 갈 때만 붙인다. /api/auth 로 좁혀서 다른 요청에는 안 실린다.
 *   Max-Age   얼마나 남길지. 안 주면 브라우저를 닫을 때 사라진다.
 * </pre>
 *
 * Secure 와 SameSite 는 개발(http://localhost)과 운영(https)에서 값이 달라야 해서
 * application.yml 의 app.refresh-cookie 로 빼뒀다. 그쪽 주석에 왜 그런지 적어뒀다.
 */
@Component
public class RefreshTokenCookie {

    @Value("${app.refresh-cookie.name}")
    private String name;

    @Value("${app.refresh-cookie.path}")
    private String path;

    @Value("${app.refresh-cookie.secure}")
    private boolean secure;

    @Value("${app.refresh-cookie.same-site}")
    private String sameSite;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Refresh Token 을 담은 쿠키를 만든다. 로그인 성공 · 토큰 재발급 때 쓴다.
     *
     * @param token Refresh Token 원문
     * @param keep  화면의 "로그인 상태 유지" 체크 여부
     *
     *              <pre>
     *   true  → Max-Age 14일. 브라우저를 껐다 켜도 남는다 (자동 로그인)
     *   false → Max-Age 없음. 브라우저를 닫으면 사라진다 (공용 PC)
     *              </pre>
     *
     *              어느 쪽이든 토큰 자체는 14일짜리다. keep 이 false 면 쿠키가 먼저
     *              사라져서 그 토큰을 꺼낼 길이 없어질 뿐이다.
     */
    public ResponseCookie create(String token, boolean keep) {
        ResponseCookie.ResponseCookieBuilder builder = base(token);

        if (keep) {
            builder.maxAge(jwtUtil.getRefreshExpiresInSeconds());
        }
        // keep 이 false 면 maxAge 를 아예 안 준다.
        // 그러면 "세션 쿠키" 가 되어 브라우저를 닫을 때 사라진다.

        return builder.build();
    }

    /**
     * 쿠키를 지우는 쿠키를 만든다. 로그아웃 때 쓴다.
     *
     * 쿠키는 서버가 "지워라" 라고 말할 수 없다. 대신 <b>같은 이름 · 같은 Path</b> 로
     * 빈 값에 Max-Age 0 을 주면 브라우저가 그 자리에서 버린다.
     *
     * 이름이나 Path 가 하나라도 다르면 지워지지 않고 쿠키가 하나 더 생긴다.
     * 그래서 만들 때와 똑같은 설정을 쓰도록 base() 를 같이 쓴다.
     */
    public ResponseCookie delete() {
        return base("").maxAge(0).build();
    }

    // 만들 때와 지울 때가 똑같아야 하는 부분
    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(name, value)
                .httpOnly(true) // 자바스크립트가 못 읽는다
                .secure(secure) // 운영(https)에서는 true
                .sameSite(sameSite) // 남의 사이트가 보낸 요청에 붙일지
                .path(path); // /api/auth 로 갈 때만 붙는다
    }

    /**
     * 들어온 요청의 쿠키에서 Refresh Token 을 꺼낸다. 없으면 null.
     *
     * 브라우저는 쿠키를 여러 개 보낼 수 있어서(테마 설정 같은 것들) 이름이 맞는 걸 찾는다.
     */
    public String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null; // 쿠키를 하나도 안 보냈다
        }

        for (int i = 0; i < cookies.length; i++) {
            if (name.equals(cookies[i].getName())) {
                return cookies[i].getValue();
            }
        }
        return null;
    }
}
