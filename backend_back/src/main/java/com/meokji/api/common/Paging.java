package com.meokji.api.common;

/**
 * 목록 페이징 계산.
 *
 * <p>왜 필요한가
 *
 * <p>페이지 목록을 주는 API 는 전부 같은 계산을 한다. "앞에서 몇 개를 건너뛸지(offset)" 와
 * "전체 몇 페이지인지(total_pages)" 다. 두 줄밖에 안 되지만 서비스마다 복사해 두면
 * 한쪽만 고치는 일이 생긴다. 실제로 HistoryServiceImpl 에 있던 계산이
 * NoticeServiceImpl · ReviewServiceImpl 에 주석으로 복사돼 있었다.
 *
 * <p>그래서 계산을 이 클래스 하나에 모았다. 목록을 주는 서비스는 이것을 가져다 쓴다.
 *
 * <p>쓰는 법 — 목록을 주는 서비스 메서드는 이 모양이 전부다
 *
 * <pre>
 * Paging paging = new Paging(page, size);              // 요청값을 받아 보정
 * paging.setTotal(noticeDAO.count());                  // 전체 개수를 알려준다
 *
 * List&lt;NoticeVO&gt; items = noticeDAO.findPage(paging.getOffset(), paging.getSize());
 *
 * Page&lt;NoticeVO&gt; result = new Page&lt;&gt;();
 * result.fill(paging, items);                          // total · page · size · total_pages 를 한 번에 채운다
 * return result;
 * </pre>
 *
 * <p>잘못된 요청값은 막지 않고 고쳐서 쓴다. page=0 이면 1페이지로, size=999 면
 * {@link #MAX_SIZE} 로 줄인다. 컨트롤러가 이미 {@code @Min} · {@code @Max} 로
 * 걸러주지만, 서비스를 직접 부르는 자리(배치 · 테스트 · 다른 서비스)가 생겨도
 * offset 이 음수가 되는 일은 없어야 한다. offset 이 음수면 SQL 이 에러를 낸다.
 */
public class Paging {

    public static final int MAX_SIZE = 50; // 한 페이지 최대 개수. 컨트롤러의 @Max 도 이 값을 쓴다
    public static final int DEFAULT_SIZE = 20; // size 를 안 보냈거나 이상한 값일 때

    private int page; // 지금 페이지 번호 (1부터)
    private int size; // 한 페이지 개수
    private int total; // 전체 개수. setTotal 로 받는다

    public Paging(int page, int size) {
        this.page = page < 1 ? 1 : page;

        if (size < 1) {
            this.size = DEFAULT_SIZE;
        } else if (size > MAX_SIZE) {
            this.size = MAX_SIZE;
        } else {
            this.size = size;
        }
    }

    // 전체 개수. DAO 로 세어서 알려준다 (이 값이 있어야 total_pages 를 계산할 수 있다)
    public void setTotal(int total) {
        this.total = total < 0 ? 0 : total;
    }

    public int getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    // 앞에서 몇 개를 건너뛸지. 20개씩 보는 2페이지면 20개를 건너뛴다.
    // SQL 의 OFFSET 자리에 그대로 넣는다.
    public int getOffset() {
        return (page - 1) * size;
    }

    // 전체 페이지 수. 45개를 20개씩 나누면 3페이지다 (20 + 20 + 5).
    //
    // 나누기만 하면 45 / 20 = 2 가 되어 마지막 5개를 볼 방법이 없어진다.
    // 그래서 나누기 전에 (size - 1) 을 더해 올림으로 만든다.
    // 0개면 0페이지다 — 화면은 이때 페이지 버튼을 아예 안 그린다.
    public int getTotalPages() {
        if (total < 1) {
            return 0;
        }
        return (total + size - 1) / size;
    }
}
