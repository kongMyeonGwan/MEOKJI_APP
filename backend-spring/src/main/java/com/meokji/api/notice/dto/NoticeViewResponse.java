package com.meokji.api.notice.dto;

/**
 * 조회수를 올린 뒤의 값.
 *
 * <p>목록 조회에서 같이 올리지 않고 따로 뺐다. 목록을 열 때마다 모든 공지의
 * 조회수가 오르면 숫자가 의미를 잃는다 - 화면은 공지를 펼칠 때 부른다.
 */
public record NoticeViewResponse(
        int viewCount) {
}
