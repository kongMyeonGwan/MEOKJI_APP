package com.meokji.api.auth.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meokji.api.common.ApiException;
import com.meokji.api.user.dao.UserDAO;
import com.meokji.api.auth.vo.LoginVO;
import com.meokji.api.auth.vo.RegisterVO;
import com.meokji.api.auth.vo.TokenVO;
import com.meokji.api.util.JwtUtil;
import com.meokji.api.util.Passwords;
import com.meokji.api.util.RegionCatalog;
import com.meokji.api.util.Times;
import com.meokji.api.user.vo.UserVO;

@Service
public class AuthServiceImpl implements AuthService {

    // 로그인 실패 문구. 이메일이 없는 경우와 비밀번호가 틀린 경우를 같은 문구로 알려준다.
    private static final String LOGIN_FAIL = "이메일 또는 비밀번호가 올바르지 않습니다.";

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private RegionCatalog regionCatalog;

    @Autowired
    private JwtUtil jwtUtil;

    // 로그인을 이 횟수만큼 틀리면 계정을 잠근다 (application.yml)
    @Value("${app.max-login-fail-count}")
    private int maxLoginFailCount;

    // ===================== 회원가입 =====================

    @Override
    @Transactional
    public UserVO register(RegisterVO request) {
        String email = request.getEmail();
        String nickname = request.getNickname().trim();

        // 비밀번호가 너무 길면(72바이트 초과) 암호화가 안 된다
        Passwords.checkBytes(request.getPassword());

        // 1. 이메일 · 닉네임이 이미 있는지 확인
        UserVO existing = userDAO.findActiveByEmailOrNickname(email, nickname);
        if (existing != null) {
            if (email.equals(existing.getEmail())) {
                throw ApiException.conflict("이미 사용 중인 이메일입니다.");
            }
            throw ApiException.conflict("이미 사용 중인 닉네임입니다.");
        }

        // 2. 저장할 회원 정보 만들기 (비밀번호는 암호화해서 저장)
        UserVO user = new UserVO();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(nickname);
        user.setGender(request.getGender());
        user.setAddressSido(request.getAddressSido());
        user.setAddressSigungu(request.getAddressSigungu());

        // 주소를 좌표로 바꿔서 같이 저장한다 (첫 화면 날씨에 쓴다)
        BigDecimal[] coords = regionCatalog.findCoords(user.getAddressSido(), user.getAddressSigungu());
        if (coords != null) {
            user.setLatitude(coords[0]);
            user.setLongitude(coords[1]);
        }

        // 3. DB 에 저장
        try {
            userDAO.insert(user);
        } catch (DuplicateKeyException e) {
            // 1번 확인과 저장 사이에 다른 사람이 먼저 가입한 경우
            String message = e.getMessage().toUpperCase();
            if (message.contains("UQ_USERS_EMAIL")) {
                throw ApiException.conflict("이미 사용 중인 이메일입니다.");
            }
            if (message.contains("UQ_USERS_NICKNAME")) {
                throw ApiException.conflict("이미 사용 중인 닉네임입니다.");
            }
            throw e;
        }

        // 4. DB 가 채운 값(id, 가입일 등)까지 다시 읽어서 돌려준다
        return userDAO.findById(user.getId());
    }

    // ===================== 로그인 =====================

    @Override
    @Transactional
    public UserVO login(LoginVO request) {
        // 1. 이메일로 회원 찾기
        UserVO user = userDAO.findActiveByEmail(request.getEmail());
        if (user == null) {
            throw ApiException.unauthorized(LOGIN_FAIL);
        }

        // 2. 잠긴 계정이면 비밀번호 확인도 안 한다
        if (user.isLockedAccount()) {
            throw ApiException.locked("계정이 잠겼습니다. 비밀번호를 재설정해주세요.");
        }

        // 3. 비밀번호 확인
        if (!checkPassword(request.getPassword(), user.getPasswordHash())) {
            loginFailed(user); // 여기서 항상 에러를 던진다
        }

        // 4. 로그인 성공. 그동안 틀린 횟수를 0 으로 되돌린다
        if (user.getLoginFailCount() != null && user.getLoginFailCount() > 0) {
            userDAO.resetLoginFailCount(user.getId());
            user.setLoginFailCount(0);
        }

        return user;
    }

    // 비밀번호가 틀렸을 때. 틀린 횟수를 올리고, 너무 많이 틀리면 계정을 잠근다.
    private void loginFailed(UserVO user) {
        int failCount = 1;
        if (user.getLoginFailCount() != null) {
            failCount = user.getLoginFailCount() + 1;
        }

        if (failCount >= maxLoginFailCount) {
            userDAO.lockAccount(user.getId());
            throw ApiException.locked(
                    "로그인 " + maxLoginFailCount + "회 실패로 계정이 잠겼습니다. 비밀번호를 재설정해주세요.");
        }

        userDAO.increaseLoginFailCount(user.getId());

        int remaining = maxLoginFailCount - failCount;
        throw ApiException.unauthorized(LOGIN_FAIL + " (남은 시도 " + remaining + "회)");
    }

    // ===================== 토큰 발급 · 재발급 · 로그아웃 =====================

    @Override
    @Transactional
    public TokenVO issueTokens(UserVO user, boolean keep) {
        // 1. Access Token (30분). DB 에 저장하지 않는다 - 서명만 맞으면 믿는다.
        String accessToken = jwtUtil.createAccessToken(user.getId());

        // 2. Refresh Token (14일).
        String refreshToken = jwtUtil.createRefreshToken(user.getId());

        // 3. DB 에는 원문이 아니라 해시를 넣는다.
        //
        //    왜 DB 에 넣나 — Access Token 과 달리 서버가 "무효로 만들 수 있어야" 하기
        //    때문이다. 로그아웃하면 이 칸을 NULL 로 되돌리고, 그러면 그 사람 브라우저에
        //    남아 있는 쿠키는 그 순간부터 쓸모가 없어진다. JWT 는 한 번 만들면 만료
        //    전까지 되돌릴 수 없어서, 이렇게 DB 를 같이 봐야 취소가 가능해진다.
        //
        //    왜 해시인가 — JwtUtil.hashRefreshToken 주석 참고.
        userDAO.updateRefreshToken(
                user.getId(),
                jwtUtil.hashRefreshToken(refreshToken),
                jwtUtil.getRefreshExpiresAt());

        TokenVO response = new TokenVO();
        response.setAccessToken(accessToken);
        response.setTokenType("bearer");
        response.setExpiresIn(jwtUtil.getExpiresInSeconds());
        response.setUser(user);
        response.setRefreshToken(refreshToken); // @JsonIgnore - 쿠키로만 나간다
        response.setKeep(keep);
        return response;
    }

    @Override
    @Transactional
    public TokenVO refresh(String refreshToken, boolean keep) {
        if (refreshToken == null || refreshToken.isBlank()) {
            // 쿠키가 아예 없다. 로그인한 적이 없거나, 로그아웃했거나, 14일이 지나
            // 브라우저가 쿠키를 버린 경우다.
            throw ApiException.unauthorized("로그인이 필요합니다.");
        }

        // 1. 토큰 자체 검사 (서명 · 만료 · 종류가 refresh 인지)
        //    여기서 Access Token 을 넣으면 null 이다 - 30분짜리로 14일 효과를 내려는
        //    시도를 막는다.
        Long userId = jwtUtil.getRefreshUserId(refreshToken);
        if (userId == null) {
            throw ApiException.unauthorized("다시 로그인해주세요.");
        }

        // 2. 그 회원이 지금도 쓸 수 있는 계정인가
        UserVO user = userDAO.findById(userId);
        if (user == null || !"Y".equals(user.getUseYn())) {
            throw ApiException.unauthorized("다시 로그인해주세요.");
        }
        if (user.isLockedAccount()) {
            throw ApiException.locked("계정이 잠겼습니다. 비밀번호를 재설정해주세요.");
        }

        // 3. DB 에 저장해둔 것과 같은 토큰인가
        //
        //    토큰 서명이 맞아도 여기서 걸릴 수 있다. 서명은 "우리가 만든 토큰" 이라는
        //    뜻일 뿐, "아직 유효하다" 는 뜻은 아니기 때문이다. 다음 경우가 걸린다.
        //      - 로그아웃했다 (DB 가 NULL 이다)
        //      - 다른 기기에서 다시 로그인했다 (DB 가 그쪽 토큰으로 덮였다)
        //      - 이미 한 번 재발급에 썼다 (회전 때문에 DB 가 새 토큰으로 바뀌었다)
        String incoming = jwtUtil.hashRefreshToken(refreshToken);
        if (user.getRefreshTokenHash() == null || !user.getRefreshTokenHash().equals(incoming)) {
            throw ApiException.unauthorized("다시 로그인해주세요.");
        }

        // 4. DB 에 적어둔 만료 시각도 본다
        //
        //    토큰 안에도 만료 시각이 있어서 2번에서 이미 걸러진다. 그래도 한 번 더
        //    보는 이유는, 이 둘이 어긋나는 상황을 DB 쪽 값으로 막기 위해서다 -
        //    예를 들어 유효기간 설정을 14일에서 1일로 줄이면, 이미 나가 있는
        //    14일짜리 토큰은 2번을 통과하지만 DB 값으로는 걸러낼 수 있다.
        LocalDateTime expiresAt = user.getRefreshExpiresAt();
        if (expiresAt == null || expiresAt.isBefore(Times.nowUtc())) {
            throw ApiException.unauthorized("로그인이 만료되었습니다. 다시 로그인해주세요.");
        }

        // 5. 통과. 토큰을 둘 다 새로 만든다 (Refresh Token Rotation)
        //
        //    Access Token 만 새로 주고 Refresh Token 은 그대로 둘 수도 있다.
        //    그런데 그러면 그 값이 14일 내내 같아서, 한 번 새면 2주 동안 계속 쓰인다.
        //
        //    회전시키면 쓸 때마다 값이 바뀐다. 공격자가 훔친 토큰을 쓰는 순간 DB 가
        //    그쪽 토큰으로 덮이고, 진짜 사용자는 다음 재발급에서 3번에 걸려 로그아웃된다.
        //    "조용히 계속 당하는" 대신 "바로 티가 나는" 쪽이 된다.
        //
        //    코드가 늘지도 않는다 - 로그인 때 쓰는 issueTokens 를 그대로 다시 부른다.
        return issueTokens(user, keep);
    }

    @Override
    @Transactional
    public void logout(Long userId) {
        userDAO.clearRefreshToken(userId);
    }

    @Override
    public Long findUserIdByRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }
        return jwtUtil.getRefreshUserId(refreshToken);
    }

    // 입력한 비밀번호와 DB 에 저장된 암호화된 비밀번호가 같은지
    private boolean checkPassword(String password, String passwordHash) {
        if (Passwords.tooLong(password)) {
            return false;
        }
        try {
            return passwordEncoder.matches(password, passwordHash);
        } catch (IllegalArgumentException e) {
            return false; // DB 의 값이 망가진 경우
        }
    }

}
