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
 *
 * <p>items 만 돌려주지 않는 이유
 *
 * <p>화면이 페이지 버튼을 그리려면 "전체가 몇 페이지인지" 를 알아야 한다. 목록만
 * 보내면 화면은 다음 페이지가 있는지 알 수 없어서, 빈 페이지를 한 번 더
 * 불러보고 나서야 끝인 걸 알게 된다. 그래서 숫자 네 칸을 같이 보낸다.
 *
 * <p>칸 이름이 totalPages 인데 응답에는 total_pages 로 나가는 이유 — 스프링 설정에서
 * JSON 칸 이름을 snake_case 로 바꾸게 해 두었다. 자바는 자바 식으로, JSON 은
 * JSON 식으로 쓰기 위한 것이고, 칸마다 @JsonProperty 를 붙이지 않아도 된다.
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
     * <p>에러가 나지 않는 실수라서 더 위험하다. 테스트도 통과하고 목록도 잘 나오는데
     * 2페이지로 넘어갈 방법만 사라진다.
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
