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
 *
 * 숫자 네 칸은 setter 로 하나씩 넣지 말고 {@link Paging} 을 만들어 fill 로 채운다.
 */
@Data
public class Page<T> {

    private int total; // 전체 개수
    private int page; // 지금 페이지 번호 (1부터)
    private int size; // 한 페이지 개수
    private int totalPages; // 전체 페이지 수
    private List<T> items; // 이 페이지의 목록

    /**
     * Paging 이 계산해 둔 숫자와 목록을 한 번에 담는다.
     *
     * <p>setter 다섯 개를 서비스마다 늘어놓지 않기 위한 메서드다. 한 칸을 빠뜨려도
     * 컴파일은 되기 때문에, 빠뜨리면 응답의 total_pages 가 0 으로 나가 화면에
     * 페이지 버튼이 사라진다. 그런 실수가 날 자리를 없앴다.
     *
     * <pre>
     * Page&lt;NoticeVO&gt; result = new Page&lt;&gt;();
     * result.fill(paging, items);
     * return result;
     * </pre>
     */
    public void Page(Paging paging, List<T> items) {
        this.total = paging.getTotal();
        this.page = paging.getPage();
        this.size = paging.getSize();
        this.totalPages = paging.getTotalPages();
        this.items = items;
    }
}
