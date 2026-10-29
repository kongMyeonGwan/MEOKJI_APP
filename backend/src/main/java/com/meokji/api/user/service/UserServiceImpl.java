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

@Service
public class UserServiceImpl implements UserService {

    private static final int NOTIFICATION_LIMIT = 20; // 알림은 최대 20개까지

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private HistoryDAO historyDAO;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private RegionCatalog regionCatalog;

    // ===================== 내 정보 =====================

    @Override
    public ProfileVO getProfile(UserVO user) {
        // 내 식사 기록 전부 (날짜, 금액만)
        List<MealHistoryVO> histories = historyDAO.findStatsRows(user.getId());

        // 기록이 있는 날짜들 (한국 시간 기준)
        Set<LocalDate> days = new HashSet<>();
        for (MealHistoryVO history : histories) {
            days.add(Kst.toDate(history.getSelectedAt()));
        }

        int streakDays = Kst.countStreak(days); // 연속 기록일
        int cookCount = histories.size(); // 요리 횟수

        // 표 한 행(UserVO) + 세어서 만든 두 칸 -> 마이페이지 모양으로 옮겨 담는다
        ProfileVO profile = new ProfileVO();
        profile.setId(user.getId());
        profile.setEmail(user.getEmail());
        profile.setNickname(user.getNickname());
        profile.setCreatedAt(Times.toText(user.getCreatedAt()));
        profile.setGender(user.getGender());
        profile.setAddressSido(user.getAddressSido());
        profile.setAddressSigungu(user.getAddressSigungu());
        profile.setAddressDong(user.getAddressDong());
        profile.setLatitude(user.getLatitude());
        profile.setLongitude(user.getLongitude());
        profile.setStreakDays(streakDays);
        profile.setCookCount(cookCount);
        return profile;
    }

    // ===================== 내 정보 수정 =====================

    @Override
    @Transactional
    public ProfileVO updateProfile(UserVO user, ProfileUpdateVO request) {
        // 닉네임과 성별만 담는다. null 인 칸은 XML 에서 건너뛴다 (UserMapper.xml 의 updateProfile)
        UserVO changes = new UserVO();
        changes.setId(user.getId());
        changes.setNickname(blankToNull(request.getNickname()));
        changes.setGender(request.getGender());

        try {
            userDAO.updateProfile(changes);
        } catch (DuplicateKeyException e) {
            if (e.getMessage().toUpperCase().contains("UQ_USERS_NICKNAME")) {
                throw ApiException.conflict("이미 사용 중인 닉네임입니다.");
            }
            throw e;
        }

        // 사는 곳은 세 칸이 한 덩어리라 따로 저장한다.
        //
        // 한 칸만 고칠 수가 없기 때문이다. 시·군·구를 바꾸면 그 아래 동은 더 이상
        // 맞지 않고, 동을 비우면 좌표가 시·군·구 중심으로 돌아가야 한다. 그래서
        // 주소를 하나라도 보냈으면 세 칸과 좌표를 통째로 다시 쓴다.
        if (request.getAddressSido() != null || request.getAddressSigungu() != null
                || request.getAddressDong() != null) {
            saveAddress(user, request);
        }

        // 바뀐 정보를 다시 읽어서 돌려준다
        return getProfile(userDAO.findById(user.getId()));
    }

    // 주소 세 칸과 좌표를 통째로 다시 쓴다.
    //   - 안 보낸 칸(null)  : 지금 값을 그대로 둔다
    //   - 빈 글자로 보낸 칸 : 비운다
    // 좌표를 못 찾으면(주소를 다 비웠을 때) NULL 이 들어간다. 그래야 날씨가
    // 브라우저로 잡은 지금 내 위치를 쓴다.
    private void saveAddress(UserVO user, ProfileUpdateVO request) {
        String sido = keepOrChange(user.getAddressSido(), request.getAddressSido());
        String sigungu = keepOrChange(user.getAddressSigungu(), request.getAddressSigungu());
        String dong = keepOrChange(user.getAddressDong(), request.getAddressDong());

        // 위가 바뀌면 아래는 버린다. "강남구 사직동" 같은 없는 주소가 남지 않게.
        if (!same(sido, user.getAddressSido()) || !same(sigungu, user.getAddressSigungu())) {
            if (request.getAddressDong() == null) {
                dong = null;
            }
        }

        BigDecimal[] coords = regionCatalog.findCoords(sido, sigungu, dong);
        BigDecimal latitude = coords == null ? null : coords[0];
        BigDecimal longitude = coords == null ? null : coords[1];

        userDAO.updateAddress(user.getId(), sido, sigungu, dong, latitude, longitude);
    }

    // 두 값이 같은지. 둘 다 null 인 것도 같은 것으로 본다.
    private boolean same(String one, String other) {
        if (one == null) {
            return other == null;
        }
        return one.equals(other);
    }

    // 안 보냈으면(null) 지금 값, 보냈으면 그 값. 빈 글자는 null 로 바꿔서 비운다.
    private String keepOrChange(String now, String sent) {
        if (sent == null) {
            return now;
        }
        return blankToNull(sent);
    }

    // 빈 글자("", "  ")는 null 로 바꾼다
    private String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    // ===================== 비밀번호 변경 =====================

    @Override
    @Transactional
    public void changePassword(UserVO user, PasswordChangeVO request) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("현재 비밀번호가 올바르지 않습니다.");
        }
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw ApiException.badRequest("지금 쓰는 비밀번호와 다른 값을 입력해주세요.");
        }

        Passwords.checkBytes(request.getNewPassword());

        userDAO.updatePassword(user.getId(), passwordEncoder.encode(request.getNewPassword()));
    }

    // ===================== 알림 =====================

    @Override
    public List<NotificationItemVO> getNotifications(UserVO user) {
        List<NotificationItemVO> items = new ArrayList<>();

        // 1. 내 글에 달린 댓글
        for (NotificationVO row : userDAO.findCommentNotifications(user.getId(), NOTIFICATION_LIMIT)) {
            NotificationItemVO item = new NotificationItemVO();
            item.setId("COMMENT-" + row.getId());
            item.setKind("COMMENT");
            item.setIcon("💬");
            item.setTitle("\"" + row.getReviewTitle() + "\"에 새 댓글이 달렸어요");
            item.setBody(row.getNickname() + ": " + shorten(row.getContent()));
            item.setCreatedAt(Times.toText(row.getCreatedAt()));
            item.setLink("/community/" + row.getReviewId());
            items.add(item);
        }

        // 2. 내 글이 받은 좋아요
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
        items.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        // 4. 최대 20개만
        if (items.size() > NOTIFICATION_LIMIT) {
            return items.subList(0, NOTIFICATION_LIMIT);
        }
        return items;
    }

    // 댓글이 길면 40자까지만 보여준다
    private String shorten(String text) {
        if (text == null) {
            return "";
        }
        String oneLine = text.replace("\n", " ").trim();
        if (oneLine.length() <= 40) {
            return oneLine;
        }
        return oneLine.substring(0, 40) + "…";
    }

    // ===================== 회원 탈퇴 =====================

    @Override
    @Transactional
    public void withdraw(UserVO user, String password) {
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw ApiException.badRequest("비밀번호가 올바르지 않습니다.");
        }

        // 행을 지우지 않고 use_yn 을 'N' 으로 바꾼다. 내가 쓴 글 · 댓글 · 기록도 같이.
        // post_comments 는 두 게시판이 같이 쓰는 표라 한 번만 부르면 둘 다 꺼진다.
        userDAO.withdrawChildren("review_likes", user.getId());
        userDAO.withdrawChildren("post_comments", user.getId());
        userDAO.withdrawChildren("recipe_reviews", user.getId());
        userDAO.withdrawChildren("free_posts", user.getId());
        userDAO.withdrawChildren("meal_histories", user.getId());
        userDAO.withdrawChildren("user_bans", user.getId());

        userDAO.withdraw(user.getId());
    }
}
