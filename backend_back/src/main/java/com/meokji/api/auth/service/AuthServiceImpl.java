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

/**
 * 회원가입 · 로그인 · 토큰으로 회원 찾기.
 *
 * <p>UserDAO 를 쓰면서 user 기능이 아니라 auth 에 있는 이유 — 표는 USERS 하나인데
 * 하는 일이 다르다. user 기능은 "이미 로그인한 사람" 의 정보를 고치고, 이쪽은
 * "아직 로그인하지 않은 사람" 을 들여보낸다. DAO 는 같이 쓰고 판단은 나눠 뒀다.
 *
 * <p>이 클래스가 지키는 두 가지 원칙
 *
 * <p>1. 실패 이유를 자세히 알려주지 않는다. 로그인에 실패했을 때 "없는 이메일"
 * 과 "비밀번호 틀림" 을 구별해 주면, 이메일만 넣어보면서 가입 여부를 하나씩
 * 확인할 수 있다.
 *
 * <p>2. 비밀번호는 되돌릴 수 없는 모양으로만 저장한다. 맞는지 볼 때도 풀어
 * 보는 게 아니라 다시 암호화해 견준다(bcrypt matches).
 */
@Service
public class AuthServiceImpl implements AuthService {

    // 로그인 실패 문구. 이메일이 없는 경우와 비밀번호가 틀린 경우를 같은 문구로 알려준다.
    //
    // 상수로 뺀 이유 — 두 자리(login 과 loginFailed)에서 쓰는데, 각자 적으면
    // 한쪽 문장만 고쳐 "없는 이메일" 과 "틀린 비밀번호" 가 구별되는 순간이 생긴다.
    private static final String LOGIN_FAIL = "이메일 또는 비밀번호가 올바르지 않습니다.";

    @Autowired
    private UserDAO userDAO;

    // WebConfig 에서 @Bean 으로 등록해 둔 것을 가져다 쓴다.
    // 여기서 new BCryptPasswordEncoder() 하면 설정(강도 등)을 바꿀 때
    // 쓰는 곳마다 고쳐야 한다.
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // 주소 -> 좌표. 가입할 때만 쓴다
    @Autowired
    private RegionCatalog regionCatalog;

    // 토큰 -> 회원 번호. 아래 getLoginUser 에서 쓴다
    @Autowired
    private JwtUtil jwtUtil;

    // 로그인을 이 횟수만큼 틀리면 계정을 잠근다 (application.yml)
    //
    // 코드에 5 를 박지 않은 이유 — 운영하면서 조이거나 풀 수 있는 값이다.
    // 숫자를 바꾸려고 다시 빌드할 일은 없어야 한다.
    @Value("${app.max-login-fail-count}")
    private int maxLoginFailCount;

    // ===================== 회원가입 =====================

    /**
     * 회원가입.
     *
     * {@code @Transactional} 이 붙은 이유 — 아래에서 ApiException 을 던지면
     * 그 전에 한 INSERT 가 되돌려진다. 안 붙이면 "회원은 들어갔는데 에러가 난"
     * 상태가 남는다.
     */
    @Override
    @Transactional
    public UserVO register(RegisterVO request) {
        String email = request.getEmail();
        // 닉네임 앞뒤 공백을 떼어낸다. " 홍길동" 과 "홍길동" 이 다른 닉네임으로
        // 들어가면 화면에서는 같아 보이는데 겹침 검사를 통과한다.
        String nickname = request.getNickname().trim();

        // 비밀번호가 너무 길면(72바이트 초과) 암호화가 안 된다
        //
        // VO 의 @Size(max = 72) 는 글자 수를 세고, 이쪽은 바이트를 센다.
        // 한글은 한 글자가 3바이트라 24글자만 넘어도 72바이트를 넘는다.
        Passwords.checkBytes(request.getPassword());

        // 1. 이메일 · 닉네임이 이미 있는지 확인
        //
        // 한 번의 SQL 로 둘을 같이 찾는다. 따로 두 번 부르면 DB 를 두 번
        // 다녀오고, 둘 다 겹쳤을 때 어느 쪽을 먼저 알려줄지가 호출 순서에 달린다.
        UserVO existing = userDAO.findActiveByEmailOrNickname(email, nickname);
        if (existing != null) {
            // 찾은 행의 이메일이 같으면 이메일이 겹친 것, 아니면 닉네임이 겹친 것
            if (email.equals(existing.getEmail())) {
                throw ApiException.conflict("이미 사용 중인 이메일입니다.");
            }
            throw ApiException.conflict("이미 사용 중인 닉네임입니다.");
        }

        // 2. 저장할 회원 정보 만들기 (비밀번호는 암호화해서 저장)
        UserVO user = new UserVO();
        user.setEmail(email);
        // encode 는 매번 다른 값을 만든다. 같은 비밀번호를 쓴 회원끼리 묶이지 않는다
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(nickname);
        // 아래 셋은 선택 항목이라 null 일 수 있다. null 을 그대로 넣는다
        user.setGender(request.getGender());
        user.setAddressSido(request.getAddressSido());
        user.setAddressSigungu(request.getAddressSigungu());

        // 주소를 좌표로 바꿔서 같이 저장한다 (첫 화면 날씨에 쓴다)
        //
        // 가입할 때 한 번 찾아 저장해 두는 이유 — 날씨를 부를 때마다 찾으면
        // 같은 계산을 매번 한다. 주소는 거의 안 바뀌는 값이다.
        BigDecimal[] coords = regionCatalog.findCoords(user.getAddressSido(), user.getAddressSigungu());
        // 주소를 안 골랐거나 목록에 없으면 null 이다. 가입은 막지 않고
        // 좌표 없이 저장한다 - 날씨만 기본 지역으로 보여주면 된다
        if (coords != null) {
            user.setLatitude(coords[0]);
            user.setLongitude(coords[1]);
        }

        // 3. DB 에 저장
        try {
            userDAO.insert(user);
        } catch (DuplicateKeyException e) {
            // 1번 확인과 저장 사이에 다른 사람이 먼저 가입한 경우
            //
            // 1번만으로 충분해 보이지만, 두 사람이 같은 이메일로 거의 같은 순간에
            // 가입하면 둘 다 1번을 통과한다. 실제로 막는 것은 DB 의 UNIQUE 제약이고,
            // 1번은 흔한 경우에 친절한 문장을 주기 위한 것이다.
            //
            // 제약 이름으로 어느 칸이 겹쳤는지 가린다. toUpperCase 는 오라클이
            // 설정에 따라 제약 이름을 소문자로 줄 수 있어서 맞춰두는 것이다.
            String message = e.getMessage().toUpperCase();
            if (message.contains("UQ_USERS_EMAIL")) {
                throw ApiException.conflict("이미 사용 중인 이메일입니다.");
            }
            if (message.contains("UQ_USERS_NICKNAME")) {
                throw ApiException.conflict("이미 사용 중인 닉네임입니다.");
            }
            // 우리가 아는 제약이 아니면 감추지 않고 그대로 올려보낸다.
            // ApiExceptionHandler 가 500 으로 처리하고 로그에 원인을 남긴다
            throw e;
        }

        // 4. DB 가 채운 값(id, 가입일 등)까지 다시 읽어서 돌려준다
        //
        // insert 후의 user 에는 시퀀스가 준 id 만 들어 있다. created_at ·
        // use_yn · role 처럼 DDL 의 DEFAULT 가 채우는 칸은 비어 있어서,
        // 그대로 돌려주면 화면에 가입일이 null 로 간다.
        return userDAO.findById(user.getId());
    }

    // ===================== 로그인 =====================

    /**
     * 로그인.
     *
     * {@code @Transactional} 이 붙은 이유 — 실패 횟수를 올리거나 계정을 잠그는
     * UPDATE 가 들어간다. 읽기만 하는 메서드가 아니다.
     *
     * 성공하든 실패하든 응답이 빠르게 돌아오지만, 실패 쪽은 DB 를 한 번 더
     * 쓴다(횟수 증가). 그래서 실패가 성공보다 느리다 — 비밀번호를 찍어보는
     * 쪽에 조금이나마 불리하다.
     */
    @Override
    @Transactional
    public UserVO login(LoginVO request) {
        // 1. 이메일로 회원 찾기
        //    findActive... 는 탈퇴하지 않은(use_yn = 'Y') 회원만 찾는다
        UserVO user = userDAO.findActiveByEmail(request.getEmail());
        if (user == null) {
            // 없는 이메일이어도 비밀번호가 틀린 것과 같은 문장을 쓴다
            throw ApiException.unauthorized(LOGIN_FAIL);
        }

        // 2. 잠긴 계정이면 비밀번호 확인도 안 한다
        //
        // 확인부터 하면, 잠긴 계정에도 비밀번호를 계속 찍어볼 수 있다.
        // 맞았을 때와 틀렸을 때 응답이 미묘하게 달라지면 그게 단서가 된다.
        if (user.isLockedAccount()) {
            throw ApiException.locked("계정이 잠겼습니다. 비밀번호를 재설정해주세요.");
        }

        // 3. 비밀번호 확인
        if (!checkPassword(request.getPassword(), user.getPasswordHash())) {
            loginFailed(user); // 여기서 항상 에러를 던진다
        }

        // 4. 로그인 성공. 그동안 틀린 횟수를 0 으로 되돌린다
        //
        // 0 이면 UPDATE 를 안 한다. 로그인은 자주 일어나는 일이라
        // 바꿀 것도 없는 UPDATE 를 매번 보낼 이유가 없다.
        if (user.getLoginFailCount() != null && user.getLoginFailCount() > 0) {
            userDAO.resetLoginFailCount(user.getId());
            // DB 만 고치면 돌려주는 user 에는 옛 횟수가 남는다.
            // 응답에 나가는 값이라 여기서도 맞춰준다
            user.setLoginFailCount(0);
        }

        return user;
    }

    /**
     * 비밀번호가 틀렸을 때. 틀린 횟수를 올리고, 너무 많이 틀리면 계정을 잠근다.
     *
     * 돌려주는 값이 없고 항상 예외를 던진다. 부르는 쪽에서 if 로 또 가리지
     * 않게 하려는 것이다 — 부른 다음 줄은 실행되지 않는다.
     */
    private void loginFailed(UserVO user) {
        // DB 에 있는 횟수 + 1 이 지금 실패 횟수다.
        // null 이면 처음 틀린 것으로 본다 (DDL 에 DEFAULT 0 이 있어 실제로는 안 온다)
        int failCount = 1;
        if (user.getLoginFailCount() != null) {
            failCount = user.getLoginFailCount() + 1;
        }

        // 한계에 닿았으면 횟수를 올리는 대신 잠근다
        if (failCount >= maxLoginFailCount) {
            userDAO.lockAccount(user.getId());
            // 몇 번 실패로 잠겼는지 알려준다. 문장에 숫자를 박지 않고
            // 설정값을 끼워 넣어서, 설정을 바꿔도 문장이 따라간다
            throw ApiException.locked(
                    "로그인 " + maxLoginFailCount + "회 실패로 계정이 잠겼습니다. 비밀번호를 재설정해주세요.");
        }

        userDAO.increaseLoginFailCount(user.getId());

        // 남은 기회를 알려준다. 알려주지 않으면 사용자는 몇 번째에 잠기는지
        // 모른 채 계정이 잠긴다
        int remaining = maxLoginFailCount - failCount;
        throw ApiException.unauthorized(LOGIN_FAIL + " (남은 시도 " + remaining + "회)");
    }

    /**
     * 입력한 비밀번호와 DB 에 저장된 암호화된 비밀번호가 같은지.
     *
     * matches 는 입력값을 저장된 해시와 같은 방식으로 다시 암호화해 견준다.
     * 해시를 풀어 원문을 꺼내는 것이 아니다 (풀 수 없다).
     */
    private boolean checkPassword(String password, String passwordHash) {
        // 72바이트를 넘으면 bcrypt 가 뒤를 잘라낸다. 그러면 앞 72바이트만
        // 같아도 통과하므로, 비교하기 전에 막는다
        if (Passwords.tooLong(password)) {
            return false;
        }
        try {
            return passwordEncoder.matches(password, passwordHash);
        } catch (IllegalArgumentException e) {
            return false; // DB 의 값이 망가진 경우
            // bcrypt 해시 모양이 아니면 matches 가 예외를 던진다. 그대로 올려보내면
            // 500 이 되는데, 이 사람은 로그인할 수 없다는 점에서 비밀번호가 틀린
            // 것과 결과가 같다. 그래서 401 로 처리한다.
        }
    }

    // ===================== 로그인한 사람 찾기 =====================

    /**
     * 토큰으로 회원을 찾는다. 안 되면 예외.
     *
     * 로그인이 꼭 필요한 컨트롤러가 첫 줄에서 이걸 부른다. 통과하면
     * 그 뒤로는 "로그인한 사람" 이 있다고 믿어도 된다.
     */
    @Override
    public UserVO getLoginUser(String token) {
        // 헤더를 아예 안 보낸 경우. 컨트롤러가 required = false 로 받으므로
        // null 이 그대로 넘어온다
        if (token == null || token.isBlank()) {
            throw ApiException.unauthorized("인증이 필요합니다.");
        }

        // 1. 토큰에서 회원 번호 꺼내기 ("Bearer " 뒤쪽이 진짜 토큰)
        //
        // 헤더 값은 "Bearer eyJhbGci..." 모양이다. replace 로 앞머리를 떼고
        // trim 으로 남은 공백을 없앤다. 서명이 안 맞거나 만료면 null 이 온다
        Long userId = jwtUtil.getUserId(token.replace("Bearer ", "").trim());
        if (userId == null) {
            throw ApiException.unauthorized("토큰이 유효하지 않거나 만료되었습니다.");
        }

        // 2. 회원 번호로 회원 찾기
        //
        // 토큰이 멀쩡해도 DB 를 한 번 더 보는 이유 — 토큰은 발급한 뒤 30분간
        // 그대로 유효하다. 그 사이에 탈퇴했거나 계정이 잠겼으면 막아야 한다.
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

    /**
     * 토큰으로 회원을 찾되, 안 되면 null.
     *
     * 게시글 목록처럼 로그인 없이도 보이지만 로그인했으면 "내가 좋아요를
     * 눌렀는지" 를 같이 표시해야 하는 자리에서 쓴다.
     *
     * getLoginUser 를 불러놓고 예외를 삼키는 모양인 이유 — 토큰 검사 ·
     * 탈퇴 확인 · 잠금 확인을 여기에 또 적으면, 한쪽만 고치는 일이 생긴다.
     */
    @Override
    public UserVO findLoginUser(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return getLoginUser(token);
        } catch (ApiException e) {
            return null; // 토큰이 잘못돼도 로그인 안 한 사람으로 본다
            // ApiException 만 잡는다. 그 밖의 에러(DB 가 죽은 경우 등)는
            // 올려보내야 한다 - 로그인 안 한 사람으로 보고 넘어가면
            // 목록이 비어 나오면서 원인을 알 수 없게 된다
        }
    }

    /**
     * 관리자인지까지 확인한다.
     *
     * getLoginUser 로 찾은 뒤 컨트롤러마다 role 을 확인하게 두면, 관리자 전용
     * 주소를 새로 만들 때 그 한 줄을 빠뜨릴 수 있다. 빠뜨려도 에러가 안 나고
     * 누구나 쓸 수 있게 열려 버리므로 하나로 묶었다.
     */
    @Override
    public UserVO getAdminUser(String token) {
        UserVO user = getLoginUser(token);
        if (!user.isAdmin()) {
            // 401 이 아니라 403 이다 - 다시 로그인해도 안 되는 경우라서
            // 화면이 로그인 페이지로 보내면 안 된다
            throw ApiException.forbidden("관리자만 할 수 있는 작업입니다.");
        }
        return user;
    }
}
