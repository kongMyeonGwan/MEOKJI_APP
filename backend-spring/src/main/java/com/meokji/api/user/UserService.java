package com.meokji.api.user;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meokji.api.auth.Passwords;
import com.meokji.api.common.ApiException;
import com.meokji.api.common.Kst;
import com.meokji.api.common.Times;
import com.meokji.api.history.HistoryMapper;
import com.meokji.api.region.RegionCatalog;
import com.meokji.api.user.dto.NotificationItem;
import com.meokji.api.user.dto.PasswordChangeRequest;
import com.meokji.api.user.dto.ProfileResponse;
import com.meokji.api.user.dto.ProfileUpdateRequest;

/**
 * 내 정보 · 알림 · 회원 탈퇴.
 *
 * <p>파이썬 쪽 {@code app/services/user_service.py} 자리다.
 */
@Service
public class UserService {

    /** 알림에 한 번에 담을 최대 건수. 상단바 패널이 그보다 길어지면 어차피 안 읽는다. */
    private static final int NOTIFICATION_LIMIT = 20;

    /**
     * 탈퇴할 때 같이 내릴 표.
     *
     * <p>자식부터 적는다. 표 이름이 SQL 에 그대로 끼워지는 자리라
     * ({@code UserMapper.xml} 의 {@code ${table}}) <b>여기 적힌 값만</b> 쓴다.
     * 사용자 입력이 섞일 길이 없어야 한다.
     */
    private static final List<String> WITHDRAW_TABLES = List.of(
            "review_likes", "review_comments", "recipe_reviews", "meal_histories", "user_bans");

    @Autowired
    private UserMapper users;
    @Autowired
    private HistoryMapper histories;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RegionCatalog regions;


    /** 내 정보. 연속 방문일 · 요리 횟수를 같이 계산해서 준다. */
    public ProfileResponse getProfile(User user) {
        List<HistoryMapper.StatsRow> rows = histories.findStatsRows(user.getId());

        Set<LocalDate> days = new HashSet<>();
        for (HistoryMapper.StatsRow row : rows) {
            days.add(Kst.toDate(row.selectedAt()));
        }

        return ProfileResponse.of(user, countStreak(days), rows.size());
    }

    /**
     * 오늘(또는 어제)부터 며칠 연속으로 기록이 있는지 센다.
     *
     * <p>어제까지 세는 이유는, 오늘 아직 아무것도 안 만든 사람의 연속 기록을
     * 0 으로 끊지 않기 위해서다. 자정을 넘긴 순간 그동안 쌓은 기록이 사라지면
     * 화면이 벌을 주는 것처럼 읽힌다.
     */
    private static int countStreak(Set<LocalDate> days) {
        if (days.isEmpty()) {
            return 0;
        }

        LocalDate today = Kst.today();
        LocalDate cursor;

        if (days.contains(today)) {
            cursor = today;
        } else if (days.contains(today.minusDays(1))) {
            cursor = today.minusDays(1);
        } else {
            return 0;
        }

        int streak = 0;
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }

        return streak;
    }

    /** 내 정보를 고친다. 보내지 않은 칸은 건드리지 않는다. */
    @Transactional
    public ProfileResponse updateProfile(User user, ProfileUpdateRequest request) {
        User changes = new User();
        changes.setId(user.getId());
        changes.setNickname(request.trimmedNickname());
        changes.setGender(request.gender());
        changes.setAddressSido(ProfileUpdateRequest.blankToNull(request.addressSido()));
        changes.setAddressSigungu(ProfileUpdateRequest.blankToNull(request.addressSigungu()));

        // 주소를 건드렸으면 좌표도 다시 맞춘다.
        // 안 하면 이사한 뒤에도 예전 동네 날씨로 추천이 나간다.
        if (request.touchesAddress()) {
            // 한쪽만 보냈으면 나머지는 지금 값을 쓴다. 시·군·구만 바꿨을 때
            // 시·도가 null 이 되어 좌표를 못 찾는 일을 막는다.
            String sido = changes.getAddressSido() != null
                    ? changes.getAddressSido() : user.getAddressSido();
            String sigungu = changes.getAddressSigungu() != null
                    ? changes.getAddressSigungu() : user.getAddressSigungu();

            BigDecimal[] coords = regions.findCoords(sido, sigungu);
            if (coords != null) {
                changes.setLatitude(coords[0]);
                changes.setLongitude(coords[1]);
            }
        }

        try {
            users.updateProfile(changes);
        } catch (DuplicateKeyException exc) {
            // uq_users_nickname 은 use_yn='Y' 인 행만 보는 함수 기반 인덱스다.
            // 탈퇴한 회원이 쓰던 닉네임은 여기 걸리지 않는다.
            if (String.valueOf(exc.getMessage()).toUpperCase().contains("UQ_USERS_NICKNAME")) {
                throw ApiException.conflict("이미 사용 중인 닉네임입니다.");
            }
            throw exc;
        }

        return getProfile(users.findById(user.getId()));
    }

    /** 로그인한 상태에서 비밀번호를 바꾼다. */
    @Transactional
    public void changePassword(User user, PasswordChangeRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("현재 비밀번호가 올바르지 않습니다.");
        }

        if (request.currentPassword().equals(request.newPassword())) {
            throw ApiException.badRequest("지금 쓰는 비밀번호와 다른 값을 입력해주세요.");
        }

        // DTO 의 @Size(max = 72) 는 글자 수라서 한글 비밀번호를 못 잡는다. 25자만
        // 넘어도 75바이트가 되어 bcrypt 가 거부하고, 그대로 두면 500 이 나간다.
        Passwords.checkBytes(request.newPassword());

        users.updatePassword(user.getId(), passwordEncoder.encode(request.newPassword()));
    }

    /**
     * 회원 탈퇴.
     *
     * <p>행을 지우지 않고 {@code use_yn='N'} 으로 바꾼다. 딸린 자료도 같이 내린다.
     *
     * <p>토큰은 따로 무효화하지 않는다. JWT 는 서버가 발급 목록을 들고 있지 않아서
     * 무효화할 방법이 없고, 대신 {@code LoginUserArgumentResolver} 가 요청마다
     * {@code use_yn} 을 확인해서 탈퇴한 회원의 토큰을 막는다.
     */
    @Transactional
    public void withdraw(User user, String password) {
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw ApiException.badRequest("비밀번호가 올바르지 않습니다.");
        }

        for (String table : WITHDRAW_TABLES) {
            users.withdrawChildren(table, user.getId());
        }

        users.withdraw(user.getId());
    }

    /** 내 글에 달린 댓글과 좋아요를 알림으로 모아준다. */
    public List<NotificationItem> listNotifications(User user) {
        List<NotificationItem> items = new ArrayList<>();

        for (UserMapper.NotificationRow row
                : users.findCommentNotifications(user.getId(), NOTIFICATION_LIMIT)) {
            items.add(new NotificationItem(
                    "COMMENT-" + row.id(),
                    "COMMENT",
                    "💬",
                    "\"" + row.reviewTitle() + "\"에 새 댓글이 달렸어요",
                    row.nickname() + ": " + shorten(row.content()),
                    Times.toText(row.createdAt()),
                    "/community/" + row.reviewId()));
        }

        for (UserMapper.NotificationRow row
                : users.findLikeNotifications(user.getId(), NOTIFICATION_LIMIT)) {
            items.add(new NotificationItem(
                    "LIKE-" + row.id(),
                    "LIKE",
                    "❤️",
                    "내 글이 좋아요를 받았어요",
                    row.nickname() + "님이 \"" + row.reviewTitle() + "\"을(를) 좋아합니다",
                    Times.toText(row.createdAt()),
                    "/community/" + row.reviewId()));
        }

        // 두 목록을 합쳤으니 다시 최신순으로 세운다.
        items.sort(Comparator.comparing(NotificationItem::createdAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        return items.size() > NOTIFICATION_LIMIT
                ? items.subList(0, NOTIFICATION_LIMIT)
                : items;
    }

    /** 알림 본문에 넣을 만큼만 자른다. */
    private static String shorten(String text) {
        if (text == null) {
            return "";
        }

        String flat = text.replace("\n", " ").trim();

        return flat.length() <= 40 ? flat : flat.substring(0, 40) + "…";
    }
}
