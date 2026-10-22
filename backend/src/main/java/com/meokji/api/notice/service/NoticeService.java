package com.meokji.api.notice.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meokji.api.common.Page;
import com.meokji.api.common.Paging;
import com.meokji.api.exception.NotFoundException;
import com.meokji.api.notice.dto.NoticeDetailResponse;
import com.meokji.api.notice.dto.NoticeListResponse;
import com.meokji.api.notice.dto.NoticeRequest;
import com.meokji.api.notice.entity.Notice;
import com.meokji.api.notice.repository.NoticeRepository;
import com.meokji.api.user.vo.UserVO;

/**
 * 공지사항. 보기는 누구나, 쓰기 · 수정 · 삭제는 관리자만이다.
 * 관리자 확인은 컨트롤러(authService.getAdminUser)가 먼저 한다.
 *
 * 트랜잭션은 클래스에 readOnly 로 걸어 두고, 쓰는 메서드에만 @Transactional 을
 * 다시 붙인다.
 */
@Service
@Transactional(readOnly = true)
public class NoticeService {

    private static final String NOT_FOUND = "공지를 찾을 수 없습니다.";

    @Autowired
    private NoticeRepository noticeRepository;

    /** 목록 한 페이지 (고정 공지가 위, 그 다음 최신순. 정렬은 Repository 쿼리 안에 있다) */
    public Page<NoticeListResponse> getList(int page, int size) {
        // ★ 요청값 보정(page=0 → 1, size=999 → 50)은 Paging 이 한다.
        // ★ 쿼리에도 보정된 값을 써야 응답의 page · size 와 실제 조회가 어긋나지 않는다
        Paging paging = new Paging(page, size);

        // Spring 의 Page 와 이름이 같아서, 아래 것은 전체 이름으로 쓴다
        // JPA 의 페이지 번호는 0 부터라 page - 1 이다. 정렬은 쿼리가 하니 Sort 는 넘기지 않는다
        org.springframework.data.domain.Page<Notice> found =
                noticeRepository.findActiveNotices(PageRequest.of(paging.getPage() - 1, paging.getSize())); // ★

        paging.setTotal((int) found.getTotalElements());

        List<NoticeListResponse> items = new ArrayList<>();
        for (Notice notice : found.getContent()) {
            items.add(new NoticeListResponse(notice));
        }

        Page<NoticeListResponse> result = new Page<>();
        result.Page(paging, items);
        return result;
    }

    /** 상세. 없거나 지워진 공지면 404. 조회수는 여기서 올리지 않는다 */
    public NoticeDetailResponse getDetail(Long noticeId) {
        return new NoticeDetailResponse(findAliveNotice(noticeId));
    }

    /** 조회수 +1. 올린 뒤의 값을 돌려준다 */
    @Transactional
    public int increaseView(Long noticeId) {
        // 지운 공지이거나 없는 공지면 바뀐 행이 0 이다
        if (noticeRepository.increaseView(noticeId) == 0) {
            throw new NotFoundException(NOT_FOUND);
        }
        return noticeRepository.findViewCount(noticeId);
    }

    /** 작성 (관리자) */
    @Transactional
    public NoticeDetailResponse add(UserVO admin, NoticeRequest request) {
        // 작성자는 토큰에서. 본문으로 받으면 남의 이름으로 쓸 수 있다
        Notice notice = Notice.create(
                admin.getId(),
                request.getTitle().trim(),
                request.getContent(),
                Boolean.TRUE.equals(request.getPinned()));

        // 바로 INSERT 를 보내야 DB 가 채운 작성 시각 · 조회수를 다시 읽어 올 수 있다
        noticeRepository.saveAndFlush(notice);

        return new NoticeDetailResponse(notice);
    }

    /** 수정 (관리자). 번호는 URL 의 값을 쓴다 */
    @Transactional
    public NoticeDetailResponse update(Long noticeId, NoticeRequest request) {
        Notice notice = findAliveNotice(noticeId);

        // 이 메서드만 부르면 UPDATE 가 나간다 (변경 감지). save() 를 다시 부르지 않는다
        notice.change(
                request.getTitle().trim(),
                request.getContent(),
                Boolean.TRUE.equals(request.getPinned()));

        return new NoticeDetailResponse(notice);
    }

    /** 삭제 (관리자). 행은 지우지 않고 use_yn='N' */
    @Transactional
    public void delete(Long noticeId) {
        // 지운 공지이거나 없는 공지면 바뀐 행이 0 이다
        if (noticeRepository.softDelete(noticeId) == 0) {
            throw new NotFoundException(NOT_FOUND);
        }
    }

    /** 살아있는 공지 하나. 없거나 지워졌으면 404 */
    private Notice findAliveNotice(Long noticeId) {
        return noticeRepository
                .findByIdAndUseYn(noticeId, "Y")
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
    }
}