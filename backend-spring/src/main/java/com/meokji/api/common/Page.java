package com.meokji.api.common;

import java.util.List;

/**
 * 목록 한 페이지를 감싸는 공통 모양.
 *
 * <p>파이썬 쪽 {@code app/schemas/page.py} 자리다. 게시판 · 히스토리 · 공지가
 * 같은 모양을 쓴다(STEP 31에서 세 곳을 한 모양으로 맞췄다).
 *
 * <p>칸 순서가 {@code total · page · size · total_pages · items} 다. record 의
 * 선언 순서가 그대로 JSON 순서가 되므로 바꾸지 않는다 - 정답지와 글자 단위로
 * 비교하는 중이다.
 */
public record Page<T>(
        int total,
        int page,
        int size,
        int totalPages,
        List<T> items) {

    /** 한 페이지에 보여줄 기본 개수. 라우터가 ?size= 로 덮어쓸 수 있다. */
    public static final int DEFAULT_SIZE = 20;

    /** 한 페이지 상한. 더 크게 요청하면 422 로 막는다. */
    public static final int MAX_SIZE = 50;

    public static <T> Page<T> of(List<T> items, int total, int page, int size) {
        return new Page<>(total, page, size, countPages(total, size), items);
    }

    /**
     * 마지막 페이지 번호.
     *
     * <p>0 건이면 0 페이지다. 1 로 두면 화면이 "1 / 1" 을 그려서, 비어 있는데도
     * 페이지가 있는 것처럼 보인다.
     */
    public static int countPages(int total, int size) {
        if (size <= 0) {
            return 0;
        }

        return (total + size - 1) / size;
    }
}
