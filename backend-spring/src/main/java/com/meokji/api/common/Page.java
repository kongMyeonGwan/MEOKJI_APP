package com.meokji.api.common;

import java.util.List;

import lombok.Data;

/**
 * 목록 한 페이지. 식사 기록, 게시판, 공지가 같이 쓴다.
 *
 * 응답 모양: { "total": 45, "page": 1, "size": 20, "total_pages": 3, "items": [...] }
 *
 * <T> 는 "items 안에 무엇이 들었는지" 를 뜻한다.
 *   Page<MealHistoryResponse> → items 가 식사 기록 목록
 */
@Data
public class Page<T> {

    public static final int DEFAULT_SIZE = 20; // 한 페이지 기본 개수
    public static final int MAX_SIZE = 50; // 한 페이지 최대 개수

    private int total; // 전체 개수
    private int page; // 지금 페이지 번호 (1부터)
    private int size; // 한 페이지 개수
    private int totalPages; // 전체 페이지 수
    private List<T> items; // 이 페이지의 목록

    public static <T> Page<T> of(List<T> items, int total, int page, int size) {
        Page<T> result = new Page<>();
        result.setItems(items);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setTotalPages(countPages(total, size));
        return result;
    }

    // 전체 페이지 수. 45개를 20개씩 나누면 3페이지. 0개면 0페이지.
    public static int countPages(int total, int size) {
        if (size <= 0) {
            return 0;
        }
        return (total + size - 1) / size;
    }
}
