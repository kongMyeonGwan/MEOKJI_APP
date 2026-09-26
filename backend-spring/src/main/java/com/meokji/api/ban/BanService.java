package com.meokji.api.ban;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meokji.api.ban.dto.BanCreateRequest;
import com.meokji.api.common.ApiException;
import com.meokji.api.user.User;

/**
 * 못 먹는 재료(Ban) 관리 (STEP 9).
 *
 * <p>파이썬 쪽 {@code app/services/ban_service.py} 자리다.
 */
@Service
public class BanService {

    private final BanMapper bans;

    public BanService(BanMapper bans) {
        this.bans = bans;
    }

    public List<UserBan> list(User user) {
        return bans.findActiveByUser(user.getId());
    }

    /**
     * Ban 을 추가한다. 같은 재료를 또 넣으면 409.
     *
     * <p>회원가입과 달리 미리 조회하지 않는다. 어느 쪽이 중복인지 가릴 것이 없고
     * (재료 하나뿐이다), 함수 기반 UNIQUE 가 이미 막아준다.
     */
    @Transactional
    public UserBan create(User user, BanCreateRequest request) {
        UserBan ban = new UserBan();
        ban.setUserId(user.getId());
        ban.setIngredientName(request.trimmedName());
        ban.setBanType(request.banTypeOrDefault());

        try {
            bans.insert(ban);
        } catch (DuplicateKeyException exc) {
            // uq_user_bans 는 use_yn='Y' 인 행만 본다. 해제한 재료는 다시 넣을 수 있다.
            if (String.valueOf(exc.getMessage()).toUpperCase().contains("UQ_USER_BANS")) {
                throw ApiException.conflict("이미 등록한 재료입니다.");
            }
            throw exc;
        }

        return bans.findById(ban.getId());
    }

    /** 내 Ban 하나를 해제한다. */
    @Transactional
    public void delete(User user, Long banId) {
        UserBan ban = bans.findById(banId);

        // 남의 Ban 을 지우려는 경우도 404 로 답한다. 403 으로 답하면 "그 id 는
        // 있다" 는 사실이 새어나간다 - 지금 화면에서 쓸 정보도 아니다.
        if (ban == null
                || !ban.getUserId().equals(user.getId())
                || !"Y".equals(ban.getUseYn())) {
            throw ApiException.notFound("등록되지 않은 재료입니다.");
        }

        bans.deactivate(banId);
    }
}
