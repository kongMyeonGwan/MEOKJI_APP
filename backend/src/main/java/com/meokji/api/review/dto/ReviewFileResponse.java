package com.meokji.api.review.dto;

import com.meokji.api.review.entity.ReviewEntity;
import lombok.Getter;

@Getter
public class ReviewFileResponse {

    private final String name;      // 사용자가 올린 원래 파일명
    private final String ext;       // 확장자 (점 없이 'jpg')
    private final Long size;        // 바이트
    private final String sizeText;  // 화면에 그대로 보여줄 크기. 예) "184 KB"
    private final String url;       // <img src> 에 넣을 주소

    public ReviewFileResponse(ReviewEntity review, String sizeText){
        this.name = review.getFileOrgName();
        this.ext = review.getFileExt();
        this.size = review.getFileSize();
        this.sizeText = sizeText;
        this.url = "/api/reviews/" + review.getId() + "/file";
    }

}
