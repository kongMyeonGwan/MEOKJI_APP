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

    public static final int MAX_SIZE = 50; // 한 페이지 최대 개수

    private int total; // 전체 개수
    private int page; // 지금 페이지 번호 (1부터)
    private int size; // 한 페이지 개수
    private int totalPages; // 전체 페이지 수
    private List<T> items; // 이 페이지의 목록
}
