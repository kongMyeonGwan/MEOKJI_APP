package com.meokji.api.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meokji.api.common.ApiException;
import com.meokji.api.config.AppProperties;
import com.meokji.api.dto.auth.LoginRequest;
import com.meokji.api.dto.auth.RegisterRequest;
import com.meokji.api.mapper.UserMapper;
import com.meokji.api.util.Passwords;
import com.meokji.api.util.RegionCatalog;
import com.meokji.api.vo.User;

/**
 * 회원가입 · 로그인.
 *
 * <p>파이썬 쪽 {@code app/services/auth_service.py} 자리다. 판단의 근거는 그쪽
 * 주석과 {@code docs/PROGRESS.md} 에 있고, 여기서는 옮길 때 달라진 부분만 적는다.
 */
@Service
public class AuthService {

    /**
     * 로그인 실패 문구.
     *
     * <p>가입되지 않은 이메일과 비밀번호가 틀린 경우를 <b>같은 문구로</b> 답한다.
     * 다만 이 서비스는 비밀번호 재설정에서 없는 이메일에 404 를 주기로 이미
     * 결정했다(STEP 32). 그래서 이 문구는 계정 열거를 막기 위한 것이 아니라,
     * 어느 쪽이 틀렸는지 화면에서 따로 안내할 이유가 없어서 하나로 둔 것이다.
     */
    private static final String INVALID_CREDENTIALS = "이메일 또는 비밀번호가 올바르지 않습니다.";

    @Autowired
    private UserMapper users;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RegionCatalog regions;
    @Autowired
    private AppProperties app;


    /** 회원가입. 이메일 · 닉네임이 겹치면 409. */
    @Transactional
    public User register(RegisterRequest request) {
        String nickname = request.trimmedNickname();

        Passwords.checkBytes(request.password());

        // 1차: 미리 조회한다. 어느 쪽이 중복인지 사용자에게 정확히 알려주기 위함이다.
        User existing = users.findActiveByEmailOrNickname(request.email(), nickname);

        if (existing != null) {
            throw request.email().equals(existing.getEmail())
                    ? ApiException.conflict("이미 사용 중인 이메일입니다.")
                    : ApiException.conflict("이미 사용 중인 닉네임입니다.");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(nickname);
        user.setGender(request.gender());
        user.setAddressSido(request.addressSido());
        user.setAddressSigungu(request.addressSigungu());

        // 고른 주소를 좌표로 바꿔 함께 저장한다. 대시보드 첫 화면의 날씨가 이 값을 쓴다.
        setCoordsFromAddress(user);

        try {
            users.insert(user);
        } catch (DuplicateKeyException exc) {
            // 2차: 미리 조회한 뒤 INSERT 하기까지 사이에 다른 요청이 끼어든 경우(경합).
            // 오라클은 ORA-00001 을 던지며 메시지에 위반된 제약조건 이름이 들어있다.
            String message = String.valueOf(exc.getMessage()).toUpperCase();

            if (message.contains("UQ_USERS_EMAIL")) {
                throw ApiException.conflict("이미 사용 중인 이메일입니다.");
            }
            if (message.contains("UQ_USERS_NICKNAME")) {
                throw ApiException.conflict("이미 사용 중인 닉네임입니다.");
            }

            // 그 외의 DB 오류는 숨기지 않고 그대로 올린다. 숨기면 원인을 못 찾는다.
            throw exc;
        }

        // DB 가 채운 id · created_at · user_level 을 읽어온다.
        // insert 는 id 만 되돌려주므로 한 번 더 조회해야 응답이 온전해진다.
        return users.findById(user.getId());
    }

    /** 이메일 + 비밀번호를 확인하고 회원을 돌려준다. */
    @Transactional
    public User login(LoginRequest request) {
        User user = users.findActiveByEmail(request.email());

        if (user == null) {
            // 가입되지 않았거나 탈퇴한 이메일. 실패 횟수를 셀 대상 자체가 없다.
            throw ApiException.unauthorized(INVALID_CREDENTIALS);
        }

        // 이미 잠긴 계정은 비밀번호 확인까지 가지 않는다.
        // 여기서 막지 않으면 잠긴 뒤에도 실패 횟수가 계속 올라간다.
        if (user.isLockedAccount()) {
            throw ApiException.locked("계정이 잠겼습니다. 비밀번호를 재설정해주세요.");
        }

        if (!matches(request.password(), user.getPasswordHash())) {
            return failLogin(user);
        }

        // 로그인 성공. 그동안 쌓인 실패 기록을 지운다.
        // 실패가 없었으면 UPDATE 를 보내지 않는다(로그인마다 쓸데없는 쓰기 방지).
        if (user.getLoginFailCount() != null && user.getLoginFailCount() != 0) {
            users.resetLoginFailCount(user.getId());
            user.setLoginFailCount(0);
        }

        return user;
    }

    /**
     * 비밀번호가 틀렸을 때. 이 메서드는 <b>항상 예외로 끝난다.</b>
     *
     * <p>반환형을 {@code User} 로 둔 것은 부르는 쪽에서 {@code return failLogin(user)} 로
     * 쓸 수 있게 하려는 것이고, 실제로 값을 돌려주지는 않는다.
     */
    private User failLogin(User user) {
        int limit = app.getMaxLoginFailCount();
        int failed = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;

        if (failed >= limit) {
            users.lockAccount(user.getId());
            throw ApiException.locked(
                    "로그인 " + limit + "회 실패로 계정이 잠겼습니다. 비밀번호를 재설정해주세요.");
        }

        users.increaseLoginFailCount(user.getId());

        // 남은 횟수를 알려주기로 한 결정 (2026-08-17).
        throw ApiException.unauthorized(
                INVALID_CREDENTIALS + " (남은 시도 " + (limit - failed) + "회)");
    }

    /** 회원의 주소를 좌표로 바꿔 latitude / longitude 에 채운다. */
    private void setCoordsFromAddress(User user) {
        BigDecimal[] coords = regions.findCoords(user.getAddressSido(), user.getAddressSigungu());

        if (coords == null) {
            user.setLatitude(null);
            user.setLongitude(null);
            return;
        }

        user.setLatitude(coords[0]);
        user.setLongitude(coords[1]);
    }

    /**
     * 평문과 저장된 해시를 대조한다.
     *
     * <p>72바이트를 넘는 입력은 대조하지 않고 실패로 본다({@link Passwords}).
     */
    private boolean matches(String rawPassword, String passwordHash) {
        if (Passwords.tooLong(rawPassword)) {
            return false;
        }

        try {
            return passwordEncoder.matches(rawPassword, passwordHash);
        } catch (IllegalArgumentException exc) {
            // DB 의 해시가 손상된 경우 등. 인증 실패로 처리한다.
            return false;
        }
    }
}
