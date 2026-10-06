package com.meokji.api.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.meokji.api.user.dao.UserDAO;
import com.meokji.api.user.vo.UserVO;
import com.meokji.api.util.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 요청 하나마다 Authorization 헤더를 보고 "누가 보낸 요청인지" 를 정하는 필터.
 *
 * <h3>필터가 뭔가</h3>
 *
 * Controller 에 도착하기 <b>전에</b> 모든 요청이 지나가는 길목이다. 예전에는 이 일을
 * Controller 마다 직접 했는데(48곳), 그러면 한 군데만 빠뜨려도 그 API 가 인증 없이 열린다.
 * 길목에서 한 번만 하면 빠뜨릴 자리가 없다.
 *
 * <pre>
 *   요청 → [이 필터] → Spring Security 인가 검사 → DispatcherServlet → Controller
 *            │
 *            └ 토큰이 멀쩡하면 "이 사람이다" 를 SecurityContext 에 올려둔다
 * </pre>
 *
 * <h3>OncePerRequestFilter 를 쓰는 이유</h3>
 *
 * 그냥 Filter 를 만들면 요청 하나에 여러 번 불릴 수 있다. 서버 안에서 다른 주소로
 * 넘기는 일(forward, 에러 화면)이 일어나면 필터 전체가 다시 돌기 때문이다. 토큰을
 * 두 번 세 번 풀 이유가 없고, 그 사이에 상태가 꼬이면 찾기 어려운 버그가 된다.
 * {@code OncePerRequestFilter} 를 상속하면 "이미 지나갔다" 는 표시를 스스로 달아
 * 요청당 정확히 한 번만 돈다.
 *
 * <h3>여기서 401 을 쓰지 않는 이유</h3>
 *
 * 토큰이 썩었다고 바로 401 을 쓰면 안 된다. 레시피 · 게시판 목록처럼
 * <b>로그인 안 해도 보이는 화면</b>이 있기 때문이다. 그런 주소에 만료된 토큰이
 * 딸려 오면, 막을 게 아니라 그냥 손님으로 보여줘야 한다.
 *
 * 그래서 이 필터는 절대 요청을 끊지 않는다. 인증에 실패하면 사유만 적어두고
 * ({@link SecurityErrors#remember}) 통과시킨다. 로그인이 필요한 주소였다면
 * 다음 단계인 Spring Security 가 막고, {@link JwtAuthenticationEntryPoint} 가
 * 그 메모를 읽어 401 을 쓴다.
 *
 * <h3>이 클래스에 @Component 를 안 붙인 이유</h3>
 *
 * Filter 를 상속한 클래스를 빈으로 만들면 스프링 부트가 <b>톰캣에도 자동으로</b>
 * 등록해버린다. 그러면 Spring Security 체인 밖에서도 한 번 더 돌아서, 보안 설정과
 * 무관한 자리에서 토큰을 풀게 된다. 그래서 빈으로 두지 않고
 * {@link SecurityConfig} 가 {@code new} 로 만들어 체인에 직접 끼운다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Authorization 헤더 값은 "Bearer eyJhbGci..." 모양이다. 앞의 이 글자를 떼면 토큰이다. */
    private static final String BEARER = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserDAO userDAO;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDAO userDAO) {
        this.jwtUtil = jwtUtil;
        this.userDAO = userDAO;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // ---------- 1. Authorization 헤더가 있는가 ----------
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith(BEARER)) {
            // 토큰을 아예 안 보냈다 = 손님이다. 사유를 적지 않는다.
            // 로그인이 필요한 주소였다면 EntryPoint 가 "인증이 필요합니다." 로 답한다.
            filterChain.doFilter(request, response);
            return;
        }

        // ---------- 2. "Bearer " 뒤쪽이 진짜 토큰이다 ----------
        String token = header.substring(BEARER.length()).trim();

        // ---------- 3. 서명 · 만료 · 종류 검사 ----------
        // getUserId 안에서 세 가지를 본다 (JwtUtil 참고).
        //   - 우리 비밀키로 서명된 게 맞는지 (내용을 고쳤으면 틀어진다)
        //   - 만료 시각이 지나지 않았는지
        //   - Access Token 인지 (Refresh Token 으로는 API 를 못 부른다)
        // 하나라도 어긋나면 null 이다.
        Long userId = jwtUtil.getUserId(token);

        if (userId == null) {
            SecurityErrors.remember(request, "토큰이 유효하지 않거나 만료되었습니다.");
            filterChain.doFilter(request, response);
            return;
        }

        // ---------- 4. 그 회원이 지금도 쓸 수 있는 계정인가 ----------
        // 토큰만 맞으면 통과시키면 안 된다. 토큰은 30분 동안 살아 있어서,
        // 받아간 뒤에 탈퇴했거나 계정이 잠겼을 수 있다. 그래서 매번 DB 를 본다.
        UserVO user = userDAO.findById(userId);

        if (user == null || !"Y".equals(user.getUseYn())) {
            // 탈퇴했거나(use_yn='N') 행이 아예 없다
            SecurityErrors.remember(request, "존재하지 않는 사용자입니다.");
            filterChain.doFilter(request, response);
            return;
        }

        if (user.isLockedAccount()) {
            // 로그인을 너무 많이 틀려 잠긴 계정이다.
            // 401 이 아니라 423(Locked) 로 답해야 화면이 "비밀번호를 재설정하세요" 를 띄운다.
            SecurityErrors.remember(request, HttpStatus.LOCKED.value(), "계정이 잠겼습니다.");
            filterChain.doFilter(request, response);
            return;
        }

        // ---------- 5. "이 사람이 맞다" 를 Spring Security 에 올린다 ----------
        //
        // Authentication 은 "지금 요청을 보낸 사람" 을 담는 상자다. 세 칸이 있다.
        //   principal   : 그 사람 자체. 우리는 UserVO 를 그대로 넣는다.
        //                 그래야 Controller 에서 @AuthenticationPrincipal UserVO 로 바로 받는다.
        //   credentials : 비밀번호. 이미 토큰으로 확인이 끝났으므로 null 이다.
        //   authorities : 권한 목록. 관리자 전용 주소를 가르는 데 쓴다.
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (user.isAdmin()) {
            // "ROLE_" 접두사는 Spring Security 의 약속이다. SecurityConfig 에서
            // hasRole("ADMIN") 이라고 쓰면 내부에서 "ROLE_ADMIN" 을 찾는다.
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, authorities);

        // SecurityContext 는 "이번 요청 동안만 쓰는 메모장" 이다. 여기에 올려두면
        // 뒤에 오는 인가 검사와 Controller 가 같은 값을 꺼내 쓴다.
        //
        // 요청마다 새로 만들어지고 끝나면 지워진다(스레드에 묶여 있다). 그래서
        // 로그인 정보가 다음 요청이나 다른 사용자에게 새지 않는다 - 서버가 로그인
        // 상태를 들고 있지 않다는 뜻이고, 이게 세션을 안 쓰는 JWT 방식의 핵심이다.
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
