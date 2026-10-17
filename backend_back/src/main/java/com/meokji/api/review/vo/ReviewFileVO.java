package com.meokji.api.review.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

/**
 * 후기에 붙은 첨부파일 한 건. 글 하나당 사진 하나다.
 *
 * RECIPE_REVIEWS 의 FILE_* 컬럼 다섯 개가 여기 담긴다. 글 VO 에 같은 칸을
 * 또 두지 않는다 — 같은 값을 두 군데 두면 한쪽만 고치는 실수가 난다.
 * (자유게시판의 FreeFileVO 와 같은 모양이다)
 *
 * 채우는 곳이 둘이다
 *   savedName · path · name · ext · size  — DB 에서 온다 (ReviewMapper 의 association)
 *   sizeText · url                        — 서비스가 계산해 넣는다
 *
 * 첨부가 없는 글이면 이 객체 자체가 null 이다. 다섯 칸이 전부 NULL 이면
 * MyBatis 가 association 을 안 만들기 때문이다.
 *
 * url 은 정적 폴더 주소가 아니라 GET /api/reviews/{id}/file 이다.
 * 업로드 폴더를 열어두면 지워진 글의 사진도 주소만 알면 계속 보인다.
 *
 * <p>칸을 둘로 나눠둔 이유 — 서버만 아는 칸(savedName · path)은 바깥으로
 * 나가면 안 된다. 저장 경로를 알려주면 폴더 구조가 드러나고, UUID 이름을
 * 알려주면 그 이름으로 바로 접근을 시도할 수 있다.
 *
 * <p>표를 나누지 않고 컬럼 다섯 개로 들고 있는 이유는 글 하나당 사진이
 * 하나라서다. 여러 장을 받게 되면 표를 따로 만들어야 한다.
 */
@Data
public class ReviewFileVO {

    // ---------- 서버만 아는 칸 ----------

    /** 디스크에 저장한 이름 (UUID). 올린 이름을 그대로 쓰면 서로 덮어쓴다 */
    @JsonIgnore
    private String savedName;

    /** 저장 폴더. 'uploads/2026/09' 처럼 파일명은 빼고 적는다 */
    @JsonIgnore
    private String path;

    // ---------- 화면에 나가는 칸 ----------

    /** 사용자가 올린 원래 파일명. 화면 표시 · 내려받기용 */
    private String name;

    /** 확장자 (점 없이 'jpg') */
    private String ext;

    /** 바이트. 크기를 못 구한 파일도 정상이라 null 일 수 있다 */
    private Long size;

    /** 화면에 그대로 보여줄 크기. 예) "184 KB" */
    private String sizeText;

    /** <img src> 에 넣을 주소 */
    private String url;
}
