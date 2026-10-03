package com.meokji.api.free.vo;

import lombok.Data;

/**
 * 첨부파일 한 건. 글 하나당 사진 하나다.
 *
 * url 은 업로드 폴더 주소가 아니라 GET /api/free-posts/{id}/file 이다.
 * 폴더를 그대로 열어두면 지워진 글의 사진도 주소만 알면 계속 보인다.
 */
@Data
public class FreeFileVO {

    /** 사용자가 올린 원래 파일명 */
    private String name;

    /** 확장자 (점 없이 'jpg') */
    private String ext;

    /** 바이트 */
    private Long size;

    /** 화면에 그대로 보여줄 크기. 예) "184 KB" */
    private String sizeText;

    /** <img src> 에 넣을 주소 */
    private String url;
}
