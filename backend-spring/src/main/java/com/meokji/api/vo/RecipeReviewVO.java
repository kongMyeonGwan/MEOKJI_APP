package com.meokji.api.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * RECIPE_REVIEWS 표 한 행. 레시피 후기(게시글).
 *
 * mealHistoryId 가 NOT NULL 이다. 후기는 자기 식사 기록에서만 쓸 수
 * 있다는 규칙을 FK 로 강제한다. 같은 기록으로 두 번 쓰는 것은
 * uq_reviews_meal 이 막는다.
 *
 * 첨부파일은 컬럼 여섯 개로 들고 있다. 파일 본체는 디스크(UPLOAD_DIR)에
 * 두고 DB 에는 경로만 적는다. 글 하나당 사진 하나라서 표를 나누지 않았다.
 */
@Data
public class RecipeReviewVO {

    private Long id;

    /** 글쓴이 */
    private Long userId;

    /** 어떤 식사의 후기인지. NOT NULL — 자기 기록만 공유할 수 있다 */
    private Long mealHistoryId;

    private String title;

    private String content;

    private Integer viewCount;

    /** 'N' = 사용자가 지운 글. 조회에는 use_yn='Y' 를 붙여야 한다 */
    private String useYn;

    /** 'Y' = 첨부파일이 있다. 목록의 has_file 이 이 값이다 */
    private String fileYn;

    /** 디스크에 저장한 이름(UUID). 사용자에게 보여주지 않는다 */
    private String fileName;

    /** 사용자가 올린 원래 이름. 화면과 내려받기에 이 이름을 쓴다 */
    private String fileOrgName;

    /** 저장 폴더('uploads/2026/09'). 월별로 나눠 한 폴더가 커지지 않게 한다 */
    private String filePath;

    /** 점 없이 'jpg' */
    private String fileExt;

    /** 바이트. 크기를 못 구한 파일도 정상이라 null 일 수 있다 */
    private Long fileSize;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
