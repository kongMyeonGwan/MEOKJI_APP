package com.meokji.api.free.dto;

import com.meokji.api.free.entity.FreePostEntity;

import lombok.Getter;

/**
 * 글에 붙은 사진 (서버 → 화면). 글 하나당 하나다.
 *
 * { "name", "ext", "size", "size_text", "url" }
 *
 * 디스크에 저장한 이름(UUID)과 폴더는 내보내지 않는다. 서버 사정이고,
 * 알려주면 업로드 폴더 구조가 드러난다.
 *
 * url 은 업로드 폴더 주소가 아니라 GET /api/free-posts/{id}/file 이다.
 * 폴더를 그대로 열어두면 지워진 글의 사진도 주소만 알면 계속 보인다.
 */
@Getter
public class FreeFileResponse {

    private final String name;      // 사용자가 올린 원래 파일명
    private final String ext;       // 확장자 (점 없이 'jpg')
    private final Long size;        // 바이트
    private final String sizeText;  // 화면에 그대로 보여줄 크기. 예) "184 KB"
    private final String url;       // <img src> 에 넣을 주소

    /** sizeText 는 FileStore.sizeText 로 서비스가 계산해 넘긴다 */
    public FreeFileResponse(FreePostEntity post, String sizeText) {
        this.name = post.getFileOrgName();
        this.ext = post.getFileExt();
        this.size = post.getFileSize();
        this.sizeText = sizeText;
        this.url = "/api/free-posts/" + post.getId() + "/file";
    }
}
