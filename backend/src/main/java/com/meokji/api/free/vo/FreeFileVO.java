package com.meokji.api.free.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

/**
 * 글에 붙은 첨부파일 한 건. 글 하나당 사진 하나다.
 *
 * FREE_POSTS 의 FILE_* 컬럼 다섯 개가 여기 담긴다. 글 VO 에 같은 칸을
 * 또 두지 않는다 — 예전에는 FreePostVO 가 fileOrgName · fileExt ·
 * fileSize 를 들고 있고 이 클래스가 그 값을 name · ext · size 로 다시
 * 받아 적었다. 같은 값을 두 군데 두면 한쪽만 고치는 실수가 난다.
 *
 * 채우는 곳이 둘이다
 *   savedName · path · name · ext · size  — DB 에서 온다 (FreePostMapper 의 postWithFile)
 *   sizeText · url                        — 서비스가 계산해 넣는다
 *
 * 첨부가 없는 글이면 이 객체 자체가 null 이다. 다섯 칸이 전부 NULL 이면
 * MyBatis 가 association 을 안 만들기 때문이다. 화면도 file 이 없으면
 * 사진 자리를 통째로 그리지 않는다.
 *
 * url 은 업로드 폴더 주소가 아니라 GET /api/free-posts/{id}/file 이다.
 * 폴더를 그대로 열어두면 지워진 글의 사진도 주소만 알면 계속 보인다.
 */
@Data
public class FreeFileVO {

    // ---------- 서버만 아는 칸 ----------

    /** 디스크에 저장한 이름 (UUID). 올린 이름을 그대로 쓰면 서로 덮어쓴다 */
    @JsonIgnore
    private String savedName;

    /** 저장 폴더. 'free/2026/10' 처럼 파일명은 빼고 적는다 */
    @JsonIgnore
    private String path;

    // ---------- 화면에 나가는 칸 ----------

    /** 사용자가 올린 원래 파일명. 화면 표시 · 내려받기용 */
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
