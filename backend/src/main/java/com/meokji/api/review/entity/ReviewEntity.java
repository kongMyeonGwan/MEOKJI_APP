package com.meokji.api.review.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "recipe_reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "meal_history_id", nullable = false, updatable = false)
    private Long meal_history_Id;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "file_yn", nullable = false, length = 1)
    private String fileYn;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_org_name")
    private String fileOrgName;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_ext")
    private String fileExt;

    @Column(name = "file_size")
    private Long fileSize;

    @Generated
    @Column(name = "view_count", insertable = false, updatable = false)
    private Integer viewCount;

    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn;

    @Generated
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public static ReviewEntity create(Long userId, Long meal_history_Id, String title,  String content) {
        ReviewEntity post = new ReviewEntity();
        post.userId = userId;
        post.title = title;
        post.meal_history_Id = meal_history_Id;
        post.content = content;
        post.fileYn = "N";
        post.useYn = "Y";
        post.updatedAt = nowUtc();
        return post;
    }

    public void change(String title, String content){
        this.title = title;
        this.content = content;
        this.updatedAt = nowUtc();
    }

    public void deactivate() {
        this.useYn = "N";
        this.updatedAt = nowUtc();
    }

    public void attachFile(String path, String savedName, String orgName, String ext, Long size) {
        this.fileYn = "Y";
        this.filePath = path;
        this.fileName = savedName;
        this.fileOrgName = orgName;
        this.fileExt = ext;
        this.fileSize = size;
        this.updatedAt = nowUtc();
    }

    public void detachFile() {
        this.fileYn = "N";
        this.filePath = null;
        this.fileName = null;
        this.fileOrgName = null;
        this.fileExt = null;
        this.fileSize = null;
        this.updatedAt = nowUtc();
    }

    public boolean hasFile() {
        return "Y".equals(fileYn);
    }

}
