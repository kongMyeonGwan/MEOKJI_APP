package com.meokji.api.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meokji.api.common.ApiException;
import com.meokji.api.dao.BanDAO;
import com.meokji.api.dto.ban.BanCreateRequest;
import com.meokji.api.dto.ban.BanResponse;
import com.meokji.api.vo.UserBanVO;
import com.meokji.api.vo.UserVO;

@Service
public class BanServiceImpl implements BanService {

    @Autowired
    private BanDAO banDAO;

    @Override
    public List<BanResponse> getList(UserVO user) {
        List<BanResponse> result = new ArrayList<>();
        for (UserBanVO ban : banDAO.findActiveByUser(user.getId())) {
            result.add(BanResponse.from(ban));
        }
        return result;
    }

    @Override
    @Transactional
    public BanResponse add(UserVO user, BanCreateRequest request) {
        // 종류를 안 보내면 DISLIKE(싫어함)
        String banType = request.getBanType();
        if (banType == null || banType.isBlank()) {
            banType = "DISLIKE";
        }

        UserBanVO ban = new UserBanVO();
        ban.setUserId(user.getId());
        ban.setIngredientName(request.getIngredientName().trim());
        ban.setBanType(banType);

        try {
            banDAO.insert(ban);
        } catch (DuplicateKeyException e) {
            // 같은 재료가 이미 있으면 DB 가 막아준다 (UQ_USER_BANS)
            if (e.getMessage().toUpperCase().contains("UQ_USER_BANS")) {
                throw ApiException.conflict("이미 등록한 재료입니다.");
            }
            throw e;
        }

        // insert 하면 ban 에 새 id 가 채워진다. 그 id 로 다시 읽어서 돌려준다.
        return BanResponse.from(banDAO.findById(ban.getId()));
    }

    @Override
    @Transactional
    public void delete(UserVO user, Long banId) {
        UserBanVO ban = banDAO.findById(banId);

        // 없거나, 남의 것이거나, 이미 지운 것이면 404
        if (ban == null || !ban.getUserId().equals(user.getId()) || !"Y".equals(ban.getUseYn())) {
            throw ApiException.notFound("등록되지 않은 재료입니다.");
        }

        // 행을 지우지 않고 use_yn 을 'N' 으로 바꾼다
        banDAO.deactivate(banId);
    }
}
