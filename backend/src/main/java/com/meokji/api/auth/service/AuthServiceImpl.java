package com.meokji.api.auth.service;

import java.math.BigDecimal;

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
import com.meokji.api.util.JwtUtil;
import com.meokji.api.util.Passwords;
import com.meokji.api.util.RegionCatalog;
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
        user.setAddressDong(request.getAddressDong());

        // 주소를 좌표로 바꿔서 같이 저장한다 (첫 화면 날씨에 쓴다)
        BigDecimal[] coords = regionCatalog.findCoords(
                user.getAddressSido(), user.getAddressSigungu(), user.getAddressDong());
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

    // ===================== 로그인한 사람 찾기 =====================

    @Override
    public UserVO getLoginUser(String token) {
        if (token == null || token.isBlank()) {
            throw ApiException.unauthorized("인증이 필요합니다.");
        }

        // 1. 토큰에서 회원 번호 꺼내기 ("Bearer " 뒤쪽이 진짜 토큰)
        Long userId = jwtUtil.getUserId(token.replace("Bearer ", "").trim());
        if (userId == null) {
            throw ApiException.unauthorized("토큰이 유효하지 않거나 만료되었습니다.");
        }

        // 2. 회원 번호로 회원 찾기
        UserVO user = userDAO.findById(userId);
        if (user == null || !"Y".equals(user.getUseYn())) {
            // 토큰을 받은 뒤에 탈퇴한 경우
            throw ApiException.unauthorized("존재하지 않는 사용자입니다.");
        }
        if (user.isLockedAccount()) {
            throw ApiException.locked("계정이 잠겼습니다.");
        }

        return user;
    }

    @Override
    public UserVO findLoginUser(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return getLoginUser(token);
        } catch (ApiException e) {
            return null; // 토큰이 잘못돼도 로그인 안 한 사람으로 본다
        }
    }

    @Override
    public UserVO getAdminUser(String token) {
        UserVO user = getLoginUser(token);
        if (!user.isAdmin()) {
            throw ApiException.forbidden("관리자만 할 수 있는 작업입니다.");
        }
        return user;
    }
}
