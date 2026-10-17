package com.meokji.api.ban.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meokji.api.common.ApiException;
import com.meokji.api.ban.dao.BanDAO;
import com.meokji.api.ban.vo.UserBanVO;
import com.meokji.api.user.vo.UserVO;

/**
 * 못 먹는 재료 목록 · 추가 · 삭제.
 *
 * <p>이 기능의 핵심은 "남의 것을 못 건드리게" 다. 표에 userId 가 있으므로
 * 목록은 SQL 로 가리고, 삭제는 읽어서 주인을 확인한다.
 *
 * <p>지운 재료를 행째로 없애지 않는 이유(use_yn = 'N') — 나중에 "예전에 뺐던
 * 재료" 를 다시 보여주거나, 사용자가 실수로 지웠을 때 되살릴 길을 남겨둔다.
 * 삭제 로그를 따로 만들지 않아도 된다.
 */
@Service
public class BanServiceImpl implements BanService {

    @Autowired
    private BanDAO banDAO;

    /**
     * 내 목록.
     *
     * {@code @Transactional} 이 없다. 읽기만 하는 메서드라 되돌릴 것이 없다.
     */
    @Override
    public List<UserBanVO> getList(UserVO user) {
        // SQL 에서 user_id 와 use_yn 을 같이 가린다. 자바에서 전부 읽어
        // 걸러내면 남의 행까지 서버 메모리에 올라온다
        return banDAO.findActiveByUser(user.getId());
    }

    /**
     * 재료 하나 추가.
     *
     * {@code @Transactional} 이 붙은 이유 — INSERT 뒤에 findById 로 다시
     * 읽는다. 둘이 한 트랜잭션 안에 있어야 방금 넣은 행을 확실히 읽는다.
     */
    @Override
    @Transactional
    public UserBanVO add(UserVO user, UserBanVO request) {
        // 종류를 안 보내면 DISLIKE(싫어함)
        //
        // VO 의 @Pattern 은 값이 null 이면 그냥 통과시킨다. 그래서 여기서
        // 기본값을 정한다. 화면이 종류 선택을 생략할 수 있게 하려는 것이다
        String banType = request.getBanType();
        if (banType == null || banType.isBlank()) {
            banType = "DISLIKE";
        }

        // 받은 VO 를 그대로 쓰지 않고 새로 만든다.
        //
        // 화면이 보낸 객체에는 id 나 useYn 이 들어 있을 수 있다. 그대로
        // insert 에 넘기면 화면이 보낸 값이 DB 로 들어간다. 쓸 칸만
        // 골라 옮겨 담으면 그럴 일이 없다
        UserBanVO ban = new UserBanVO();
        // 회원 번호는 화면이 보낸 값이 아니라 토큰에서 온 값을 쓴다
        ban.setUserId(user.getId());
        // 앞뒤 공백을 떼어낸다. " 오이" 와 "오이" 가 다른 재료로 들어가면
        // 화면에서는 같아 보이는데 겹침 검사(UNIQUE)를 통과한다
        ban.setIngredientName(request.getIngredientName().trim());
        ban.setBanType(banType);

        try {
            banDAO.insert(ban);
        } catch (DuplicateKeyException e) {
            // 같은 재료가 이미 있으면 DB 가 막아준다 (UQ_USER_BANS)
            //
            // 넣기 전에 SELECT 로 확인하지 않는 이유 — 확인과 저장 사이에
            // 같은 요청이 또 들어오면 둘 다 통과한다. 어차피 DB 가 막아주므로
            // SELECT 를 한 번 더 할 이유가 없다
            if (e.getMessage().toUpperCase().contains("UQ_USER_BANS")) {
                throw ApiException.conflict("이미 등록한 재료입니다.");
            }
            // 우리가 아는 제약이 아니면 감추지 않고 올려보낸다 (500 + 로그)
            throw e;
        }

        // insert 하면 ban 에 새 id 가 채워진다. 그 id 로 다시 읽어서 돌려준다.
        //
        // 다시 읽는 이유 — created_at 은 DDL 의 DEFAULT 가 채우고, use_yn 도
        // DB 가 'Y' 로 넣는다. ban 을 그대로 돌려주면 화면에 등록일이 null 로 간다
        return banDAO.findById(ban.getId());
    }

    /**
     * 재료 하나 해제.
     *
     * {@code @Transactional} 이 붙은 이유 — SELECT 로 주인을 확인하고
     * UPDATE 를 한다. 둘이 떨어져 있으면 사이에 상태가 바뀔 수 있다.
     */
    @Override
    @Transactional
    public void delete(UserVO user, Long banId) {
        UserBanVO ban = banDAO.findById(banId);

        // 없거나, 남의 것이거나, 이미 지운 것이면 404
        //
        // 세 경우를 한 문장으로 묶은 이유 — 남의 것일 때 403("권한 없음")을
        // 주면, id 를 하나씩 넣어보면서 "그 번호의 재료가 있는지" 를 알아낼 수
        // 있다. 404 로 맞추면 없는 것과 남의 것이 구별되지 않는다.
        //
        // 이미 지운 것도 404 다. 두 번 눌러도 같은 결과가 나오므로
        // 화면이 따로 처리할 게 없다
        if (ban == null || !ban.getUserId().equals(user.getId()) || !"Y".equals(ban.getUseYn())) {
            throw ApiException.notFound("등록되지 않은 재료입니다.");
        }

        // 행을 지우지 않고 use_yn 을 'N' 으로 바꾼다
        // 되살릴 길을 남겨두려는 것이다 (클래스 주석 참고)
        banDAO.deactivate(banId);
    }
}
