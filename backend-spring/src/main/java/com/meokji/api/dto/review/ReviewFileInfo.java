package com.meokji.api.dto.review;

/**
 * 첨부파일 한 건. 글 하나당 사진 하나다.
 *
 * <p>{@code url} 은 정적 폴더 주소가 아니라 {@code GET /api/reviews/{id}/file} 이다.
 * 업로드 폴더를 열어두면 지워진 글의 사진도 주소만 알면 계속 보인다.
 */
public record ReviewFileInfo(
        String name,
        String ext,
        Long size,
        String sizeText,
        String url) {
}
