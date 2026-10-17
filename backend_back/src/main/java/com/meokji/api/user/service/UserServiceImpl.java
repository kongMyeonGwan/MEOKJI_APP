package com.meokji.api.user.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meokji.api.common.ApiException;
import com.meokji.api.history.dao.HistoryDAO;
import com.meokji.api.user.dao.UserDAO;
import com.meokji.api.user.vo.NotificationItemVO;
import com.meokji.api.user.vo.PasswordChangeVO;
import com.meokji.api.user.vo.ProfileUpdateVO;
import com.meokji.api.user.vo.ProfileVO;
import com.meokji.api.util.Kst;
import com.meokji.api.util.Passwords;
import com.meokji.api.util.RegionCatalog;
import com.meokji.api.util.Times;
import com.meokji.api.history.vo.MealHistoryVO;
import com.meokji.api.user.vo.NotificationVO;
import com.meokji.api.user.vo.UserVO;

/**
 * 내 정보 · 알림 · 회원 탈퇴.
 *
 * <p>USERS 표를 auth 기능과 같이 쓴다. 나눈 기준은 "로그인 전인가 후인가" 다 -
 * auth 는 아직 로그인하지 않은 사람을 들여보내고, 이쪽은 이미 로그인한
 * 사람의 정보를 다룬다. 그래서 메서드마다 UserVO 를 받는다.
 *
 * <p>DAO 를 둘 쓴다. UserDAO 말고 HistoryDAO 도 보는 이유는 마이페이지가
 * 연속 기록일과 요리 횟수를 같이 보여주기 때문이다 — 그 값은 USERS 에
 * 없고 식사 기록을 세어야 나온다.
 *
 * <p>UserVO 를 이미 받았는데 DB 를 또 보는 자리가 있다. 정보를 고친 뒤에는
 * 받은 UserVO 가 옛 값이라 다시 읽어야 한다.
 */
@Service
public class UserServiceImpl implements UserService {

    private static final int NOTIFICATION_LIMIT = 20; // 알림은 최대 20개까지
    // 댓글과 좋아요를 각각 20개씩 읽어 합친 뒤, 다시 20개로 자른다.
    // 두 번 쓰이는 값이라 상수로 뺐다

    @Autowired
    private UserDAO userDAO;

    // 연속 기록일 · 요리 횟수를 세려고 쓴다. HistoryService 가 아니라
    // DAO 를 바로 쓰는 이유는 필요한 것이 행 목록뿐이라서다
    @Autowired
    private HistoryDAO historyDAO;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // 주소를 바꿨을 때 좌표를 다시 찾는 데 쓴다
    @Autowired
    private RegionCatalog regionCatalog;

    // ===================== 내 정보 =====================

    /**
     * 마이페이지에 띄울 내 정보.
     *
     * {@code @Transactional} 이 없다. 읽기만 한다.
     *
     * 받은 UserVO 에 없는 칸이 둘이다(연속 기록일 · 요리 횟수). 그 둘을
     * 세려고 식사 기록을 읽는다.
     */
    @Override
    public ProfileVO getProfile(UserVO user) {
        // 내 식사 기록 전부 (날짜, 금액만)
        //
        // COUNT 로 세지 않고 행을 받아오는 이유 — 연속 기록일은 "날짜가
        // 하루씩 이어지는지" 를 날짜 순서대로 훑어야 알 수 있다.
        // 어차피 읽을 거라면 개수도 그 목록으로 세면 된다
        List<MealHistoryVO> histories = historyDAO.findStatsRows(user.getId());

        // 기록이 있는 날짜들 (한국 시간 기준)
        //
        // Set 인 이유 — 하루에 두 끼를 기록해도 "기록한 날" 은 하루다.
        // 목록이면 둘 다 보여야 하지만, 연속일을 셀 때는 한 번만 센다
        Set<LocalDate> days = new HashSet<>();
        for (MealHistoryVO history : histories) {
            // DB 에는 UTC 로 쌓여 있다. 그대로 날짜만 떼면 밤 9시 이후의
            // 기록이 다음 날로 넘어간다(UTC + 9 = KST)
            days.add(Kst.toDate(history.getSelectedAt()));
        }

        int streakDays = Kst.countStreak(days); // 연속 기록일
        int cookCount = histories.size(); // 요리 횟수
        // 날짜 수가 아니라 기록 수다. 하루에 두 끼면 2 로 센다 -
        // "몇 번 요리했는가" 라서 날짜로 묶으면 안 된다

        // 표 한 행(UserVO) + 세어서 만든 두 칸 -> 마이페이지 모양으로 옮겨 담는다
        //
        // UserVO 를 그대로 돌려주지 않는 이유 — 비밀번호 해시 · 실패 횟수 ·
        // 잠금 상태가 들어 있고, 세어서 만든 두 칸이 들어갈 자리가 없다
        ProfileVO profile = new ProfileVO();
        profile.setId(user.getId());
        profile.setEmail(user.getEmail());
        profile.setNickname(user.getNickname());
        // 여기서 글자로 바꾼다. ProfileVO 의 createdAt 은 String 이다
        profile.setCreatedAt(Times.toText(user.getCreatedAt()));
        profile.setGender(user.getGender());
        profile.setAddressSido(user.getAddressSido());
        profile.setAddressSigungu(user.getAddressSigungu());
        // 좌표는 UserVO 에서 @JsonIgnore 로 막혀 있지만, 마이페이지에서는
        // 내보낸다. 추천을 부를 때 화면이 실어 보낼 수 있다
        profile.setLatitude(user.getLatitude());
        profile.setLongitude(user.getLongitude());
        profile.setStreakDays(streakDays);
        profile.setCookCount(cookCount);
        return profile;
    }

    // ===================== 내 정보 수정 =====================

    /**
     * 보낸 칸만 고친다 (PATCH).
     *
     * {@code @Transactional} 이 붙은 이유 — UPDATE 뒤에 다시 읽는다.
     * 둘이 한 트랜잭션 안에 있어야 방금 고친 값을 읽는다.
     */
    @Override
    @Transactional
    public ProfileVO updateProfile(UserVO user, ProfileUpdateVO request) {
        // 바꿀 값만 담는다. null 인 칸은 XML 에서 건너뛴다 (UserMapper.xml 의 updateProfile)
        //
        // 받은 request 를 DAO 에 그대로 넘기지 않는 이유 — DAO 는 UserVO 를
        // 받는다. 여기서 옮겨 담으면서 회원 번호(토큰에서 온 값)도 같이 넣는다
        UserVO changes = new UserVO();
        changes.setId(user.getId());
        // blankToNull 을 거치는 이유 — 화면이 빈 칸을 ""(빈 글자)로 보낼 수
        // 있다. 그대로 넣으면 닉네임이 빈 값으로 바뀐다. null 로 바꾸면
        // "안 고침" 이 된다
        changes.setNickname(blankToNull(request.getNickname()));
        // 성별은 blankToNull 을 안 거친다. 드롭다운에서 고르는 값이라
        // 빈 글자가 올 일이 없고, @Pattern 이 이미 막고 있다
        changes.setGender(request.getGender());
        changes.setAddressSido(blankToNull(request.getAddressSido()));
        changes.setAddressSigungu(blankToNull(request.getAddressSigungu()));

        // 주소를 바꿨으면 좌표도 다시 계산한다 (날씨 추천에 쓴다)
        //
        // changes 가 아니라 request 를 보고 가린다 - blankToNull 을 거친
        // changes 는 ""를 보낸 경우에도 null 이라, 주소를 지우려는 요청을
        // 구별할 수 없다
        if (request.getAddressSido() != null || request.getAddressSigungu() != null) {
            // 한쪽만 보냈으면 나머지는 지금 값을 쓴다
            //
            // 시·군·구만 바꾼 경우에도 시·도가 있어야 좌표를 찾을 수 있다.
            // 안 채우면 null 로 넘어가 좌표를 못 찾는다
            String sido = changes.getAddressSido();
            if (sido == null) {
                sido = user.getAddressSido();
            }
            String sigungu = changes.getAddressSigungu();
            if (sigungu == null) {
                sigungu = user.getAddressSigungu();
            }

            BigDecimal[] coords = regionCatalog.findCoords(sido, sigungu);
            // 못 찾으면 좌표를 안 건드린다. 옛 좌표가 남지만, null 로
            // 덮어쓰면 날씨가 통째로 기본 지역이 된다
            if (coords != null) {
                changes.setLatitude(coords[0]);
                changes.setLongitude(coords[1]);
            }
        }

        try {
            userDAO.updateProfile(changes);
        } catch (DuplicateKeyException e) {
            // 미리 확인하지 않고 DB 가 막게 두었다. 확인과 UPDATE 사이에
            // 다른 사람이 같은 닉네임을 쓸 수 있어서, 어차피 여기서 한 번
            // 더 받아야 한다
            if (e.getMessage().toUpperCase().contains("UQ_USERS_NICKNAME")) {
                throw ApiException.conflict("이미 사용 중인 닉네임입니다.");
            }
            // 우리가 아는 제약이 아니면 감추지 않고 올려보낸다 (500 + 로그)
            throw e;
        }

        // 바뀐 정보를 다시 읽어서 돌려준다
        //
        // 받은 user 는 고치기 전 값이다. 그대로 getProfile 에 넘기면
        // 방금 바꾼 닉네임이 옛 값으로 나간다
        return getProfile(userDAO.findById(user.getId()));
    }

    // 빈 글자("", "  ")는 null 로 바꾼다
    //
    // 왜 필요한가 — PATCH 에서 null 은 "안 고침" 이다. 화면이 입력칸을
    // 비운 채로 보내면 ""가 오는데, 그것을 그대로 넣으면 닉네임이 사라진다.
    // 둘을 같게 다루려고 여기서 맞춘다.
    //
    // trim 까지 하는 이유 — " 홍길동" 과 "홍길동" 이 다른 닉네임으로
    // 들어가면 화면에서는 같아 보이는데 겹침 검사를 통과한다
    private String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    // ===================== 비밀번호 변경 =====================

    /**
     * 로그인한 상태에서 비밀번호 바꾸기.
     *
     * {@code @Transactional} 이 붙은 이유 — UPDATE 가 들어간다. 확인에
     * 실패하면 예외가 던져지고 아무것도 바뀌지 않는다.
     */
    @Override
    @Transactional
    public void changePassword(UserVO user, PasswordChangeVO request) {
        // 토큰이 있으면 로그인한 사람은 맞지만, 자리를 비운 사이에 누가
        // 그 브라우저를 쓸 수 있다. 비밀번호를 바꾸면 계정을 넘겨주는
        // 셈이라 한 번 더 확인한다
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("현재 비밀번호가 올바르지 않습니다.");
        }
        // 같은 값으로 바꾸는 것을 막는다. 에러는 아니지만, 사용자가
        // "바꿨다" 고 생각하고 넘어가면 안 된다
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw ApiException.badRequest("지금 쓰는 비밀번호와 다른 값을 입력해주세요.");
        }

        // VO 의 @Size(max = 72) 는 글자 수를 세고, 이쪽은 바이트를 센다.
        // 한글은 한 글자가 3바이트라 24글자만 넘어도 72바이트를 넘는다
        Passwords.checkBytes(request.getNewPassword());

        // updatePassword 를 쓴다. resetPasswordAndUnlock 가 아니다 -
        // 이 길로는 계정 잠금이 풀리면 안 된다 (UserDAO 주석 참고)
        userDAO.updatePassword(user.getId(), passwordEncoder.encode(request.getNewPassword()));
    }

    // ===================== 알림 =====================

    /**
     * 내 글에 달린 댓글 · 좋아요를 모아 최신순으로.
     *
     * {@code @Transactional} 이 없다. 읽기만 한다.
     *
     * 알림 표가 따로 없다. 댓글과 좋아요를 그때그때 읽어 만든다 —
     * 표를 만들면 같은 사실이 두 군데 쌓이고 한쪽만 지우는 일이 생긴다.
     * 대신 "읽음 표시" 를 할 수 없다.
     */
    @Override
    public List<NotificationItemVO> getNotifications(UserVO user) {
        List<NotificationItemVO> items = new ArrayList<>();

        // 1. 내 글에 달린 댓글
        //
        // NotificationVO(DB 에서 온 모양) -> NotificationItemVO(화면에 그릴 모양).
        // 아이콘 · 제목 · 본문 · 주소를 서버가 만들어 넣는다 - 화면마다
        // 조립하면 같은 알림이 화면에 따라 다르게 보인다
        for (NotificationVO row : userDAO.findCommentNotifications(user.getId(), NOTIFICATION_LIMIT)) {
            NotificationItemVO item = new NotificationItemVO();
            // 댓글 번호와 좋아요 번호가 겹칠 수 있어서 종류를 앞에 붙인다
            item.setId("COMMENT-" + row.getId());
            item.setKind("COMMENT");
            item.setIcon("💬");
            item.setTitle("\"" + row.getReviewTitle() + "\"에 새 댓글이 달렸어요");
            // 댓글이 길면 잘라서 넣는다. 안 자르면 알림 한 줄이 화면을 덮는다
            item.setBody(row.getNickname() + ": " + shorten(row.getContent()));
            item.setCreatedAt(Times.toText(row.getCreatedAt()));
            item.setLink("/community/" + row.getReviewId());
            items.add(item);
        }

        // 2. 내 글이 받은 좋아요
        //
        // 좋아요에는 남길 말이 없어서(content 가 null) 글 제목을 본문
        // 자리에 넣는다
        for (NotificationVO row : userDAO.findLikeNotifications(user.getId(), NOTIFICATION_LIMIT)) {
            NotificationItemVO item = new NotificationItemVO();
            item.setId("LIKE-" + row.getId());
            item.setKind("LIKE");
            item.setIcon("❤️");
            item.setTitle("내 글이 좋아요를 받았어요");
            item.setBody(row.getNickname() + "님이 \"" + row.getReviewTitle() + "\"을(를) 좋아합니다");
            item.setCreatedAt(Times.toText(row.getCreatedAt()));
            item.setLink("/community/" + row.getReviewId());
            items.add(item);
        }

        // 3. 최신순으로 정렬 (createdAt 글자가 클수록 최신)
        //
        // 글자로 비교해도 되는 이유 — Times.toText 가 만드는 모양이
        // "2026-10-04T12:30:00Z" 라서, 연 · 월 · 일 · 시 순으로 자리가
        // 고정돼 있다. 글자 순서가 곧 시간 순서다.
        //
        // b 와 a 를 바꿔 넣어 내림차순(최신이 앞)으로 만든다
        items.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        // 4. 최대 20개만
        //
        // 댓글 20개 + 좋아요 20개 = 최대 40개가 모였을 수 있다.
        // 섞어서 정렬한 뒤에 잘라야 "최근 20건" 이 된다 - 각각 10개씩
        // 읽으면 댓글만 많은 사람의 최근 좋아요가 안 보인다
        if (items.size() > NOTIFICATION_LIMIT) {
            return items.subList(0, NOTIFICATION_LIMIT);
        }
        return items;
    }

    // 댓글이 길면 40자까지만 보여준다
    //
    // 줄바꿈을 공백으로 바꾸는 이유 — 알림은 한 줄짜리라, 줄바꿈이
    // 들어 있으면 화면에서 줄이 깨지거나 뒷부분이 잘려 보인다
    private String shorten(String text) {
        // 좋아요 알림은 content 가 null 이다. 이 메서드는 댓글에만 쓰지만,
        // null 이 와도 터지지 않게 받아둔다
        if (text == null) {
            return "";
        }
        String oneLine = text.replace("\n", " ").trim();
        if (oneLine.length() <= 40) {
            return oneLine;
        }
        // 말줄임표를 붙여 "잘렸다" 를 알려준다. 안 붙이면 원래 그만큼인 줄 안다
        return oneLine.substring(0, 40) + "…";
    }

    // ===================== 회원 탈퇴 =====================

    /**
     * 회원 탈퇴. 행을 지우지 않고 use_yn 을 N 으로 바꾼다.
     *
     * {@code @Transactional} 이 꼭 필요하다 — UPDATE 를 일곱 번 보낸다.
     * 중간에 실패하면 "글은 내려갔는데 계정은 살아있는" 상태가 남는다.
     */
    @Override
    @Transactional
    public void withdraw(UserVO user, String password) {
        // 되돌리기 어려운 일이라 비밀번호를 한 번 더 확인한다
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw ApiException.badRequest("비밀번호가 올바르지 않습니다.");
        }

        // 행을 지우지 않고 use_yn 을 'N' 으로 바꾼다. 내가 쓴 글 · 댓글 · 기록도 같이.
        // post_comments 는 두 게시판이 같이 쓰는 표라 한 번만 부르면 둘 다 꺼진다.
        //
        // 딸린 자료부터 내리고 계정을 마지막에 내린다. 순서가 반대면
        // 중간에 실패했을 때 계정은 없는데 글은 남은 상태가 된다.
        //
        // 표 이름을 손으로 적어 넘긴다. 사용자 입력이 아니라 여기 적힌
        // 목록에서만 오므로 SQL 에 섞일 값이 아니다
        userDAO.withdrawChildren("review_likes", user.getId());
        userDAO.withdrawChildren("post_comments", user.getId());
        userDAO.withdrawChildren("recipe_reviews", user.getId());
        userDAO.withdrawChildren("free_posts", user.getId());
        userDAO.withdrawChildren("meal_histories", user.getId());
        userDAO.withdrawChildren("user_bans", user.getId());

        userDAO.withdraw(user.getId());
    }
}
