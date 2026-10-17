package com.meokji.api.free.entity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.hibernate.annotations.Generated;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FREE_POSTS 표 한 행 — JPA 가 영속성을 관리하는 대상.
 *
 * 화면과 주고받는 모양(free/dto 의 Request · Response)과 역할이 다르다.
 * 이쪽은 표 한 행 그대로다. 응답에는 표에 없는 칸(닉네임 · 댓글 · is_mine)이
 * 들어가므로 엔티티를 그대로 내보내지 않는다.
 *
 * 글쓴이는 @ManyToOne 이 아니라 userId 숫자로 둔다
 *
 *   USERS 는 MyBatis 가 관리하는 표라 User 엔티티가 없다. 글쓴이 확인은
 *   번호 비교로 충분하고, 닉네임은 FreePostRepository 의 네이티브 쿼리가
 *   JOIN 으로 가져온다. 연관관계를 걸지 않으니 LAZY 로딩 · N+1 을 신경 쓸
 *   일도 없다.
 *
 * insertable · updatable = false 를 붙인 칸
 *
 *   viewCount  조회수는 FreePostRepository.increaseView 가 view_count + 1 로만
 *              올린다. 여기서 같이 써버리면, 수정하는 사이에 누가 글을 열었을
 *              때 그 조회수가 옛 값으로 되돌아간다.
 *   createdAt  DB 기본값 SYS_EXTRACT_UTC(SYSTIMESTAMP) 가 채운다.
 *
 *   두 칸에 @Generated 를 붙였다. INSERT 직후 하이버네이트가 DB 가 채운 값을
 *   다시 읽어 와서, 글쓰기 응답에 작성 시각과 조회수 0 이 바로 담긴다.
 *
 * 첨부파일 칸 (FILE_* 여섯 개)
 *
 *   표의 ck_free_posts_file_set 제약이 "file_yn='Y' 이면 네 칸이 다 차 있고,
 *   'N' 이면 다섯 칸이 다 비어 있을 것" 을 요구한다. 그래서 여섯 칸을 각각
 *   바꾸는 setter 를 두지 않고 attachFile · detachFile 두 메서드로만 바꾼다.
 *
 * 시각은 UTC 다
 *
 *   DB 가 UTC 로 적고(위 기본값), 커넥션도 UTC 로 맞춰져 있다.
 *   그래서 수정 시각을 넣을 때 LocalDateTime.now() 를 쓰면 안 된다 —
 *   그 값은 한국 시간이라 아홉 시간 뒤로 적힌다. now(ZoneOffset.UTC) 다.
 */
// 롬복 두 줄의 뜻
//   @Getter      : setter 는 만들지 않는다. 값을 바꾸는 길을 change ·
//                  deactivate 로만 열어두려는 것이다. setter 가 있으면
//                  어디서든 한 칸씩 바꿀 수 있어 updated_at 을 빠뜨린다
//   @NoArgsConstructor(PROTECTED) : JPA 는 빈 생성자가 있어야 객체를
//                  만들 수 있다. PROTECTED 로 좁혀서 바깥에서는
//                  new FreePostEntity() 를 못 쓰게 한다 - 만들 때는
//                  아래 create 를 거쳐야 한다
@Entity
@Table(name = "free_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FreePostEntity {

    /** FREE_SEQ 시퀀스로 채운다 (db/11_free_posts_to_sequence.sql) */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "free_seq_generator")
    @SequenceGenerator(
            name = "free_seq_generator", // Java 안에서 부르는 이름
            sequenceName = "FREE_SEQ",   // Oracle 시퀀스 이름
            allocationSize = 1)
    private Long id;

    /** 글쓴이. 한 번 정해지면 안 바뀐다 */
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    /** 글 제목. nullable = false 는 DDL 과 맞춰 적은 것이다 */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** CLOB 이다. 4000자까지 받으므로 VARCHAR2 로는 안 들어간다 */
    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    // ---------- 첨부파일. 글 하나당 하나다 ----------

    /** 'Y' = 첨부 있음 */
    @Column(name = "file_yn", nullable = false, length = 1)
    private String fileYn;

    /** 디스크에 저장한 이름 (UUID) */
    @Column(name = "file_name")
    private String fileName;

    /** 사용자가 올린 원래 파일명 */
    @Column(name = "file_org_name")
    private String fileOrgName;

    /** 저장 폴더. 파일명은 빼고 적는다 */
    @Column(name = "file_path")
    private String filePath;

    /** 확장자 (점 없이 'jpg') */
    @Column(name = "file_ext")
    private String fileExt;

    /** 바이트 */
    @Column(name = "file_size")
    private Long fileSize;

    // ---------- 나머지 ----------

    /** 읽기만 한다. 올리는 것은 FreePostRepository.increaseView 다 */
    @Generated
    @Column(name = "view_count", insertable = false, updatable = false)
    private Integer viewCount;

    /** 'N' = 지운 글. 행을 지우지 않으므로 이 칸으로 가린다 */
    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn;

    /** 읽기만 한다. DB 기본값이 넣는다 */
    @Generated
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 고친 시각. 이 칸만 JPA 가 직접 넣는다.
     *
     * DB 기본값으로 둘 수 없다 - 기본값은 INSERT 때 한 번만 찍힌다.
     * 고칠 때마다 새로 적어야 하므로 change · deactivate 가 같이 넣는다.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 새 글. id · 조회수 · 작성 시각은 DB 가 채운다
     *
     * 생성자 대신 static 메서드로 둔 이유 — 이름이 하는 일을 말해준다.
     * new FreePostEntity(userId, title, content) 는 "무엇을 하는 것인지" 를
     * 인자 순서로만 알 수 있다. 또 setter 를 열지 않고도 칸을 채울 수 있다
     * (같은 클래스 안이라 private 칸에 바로 넣을 수 있다).
     */
    public static FreePostEntity create(Long userId, String title, String content) {
        FreePostEntity post = new FreePostEntity();
        post.userId = userId;
        post.title = title;
        post.content = content;
        post.fileYn = "N";
        post.useYn = "Y";
        post.updatedAt = nowUtc();
        return post;
    }

    /**
     * 제목 · 내용 고치기.
     *
     * 이 메서드만 부르면 UPDATE 가 나간다. 트랜잭션이 끝날 때 JPA 가
     * 처음 읽어둔 값과 견주어 달라진 칸을 찾아 스스로 보낸다(변경 감지).
     * save() 를 다시 부를 필요가 없다.
     */
    public void change(String title, String content) {
        this.title = title;
        this.content = content;
        this.updatedAt = nowUtc();
    }

    /**
     * 삭제. 행을 지우지 않고 use_yn='N' 으로 바꾼다.
     *
     * delete() 를 쓰면 행이 사라져서 댓글 · 좋아요가 가리킬 곳을 잃는다.
     */
    public void deactivate() {
        this.useYn = "N";
        this.updatedAt = nowUtc();
    }

    /** 사진 붙이기. 이미 있으면 덮어쓴다 (옛 파일을 디스크에서 지우는 것은 서비스 몫) */
    public void attachFile(String path, String savedName, String orgName, String ext, Long size) {
        this.fileYn = "Y";
        this.filePath = path;
        this.fileName = savedName;
        this.fileOrgName = orgName;
        this.fileExt = ext;
        this.fileSize = size;
        this.updatedAt = nowUtc();
    }

    /** 사진 떼기. 여섯 칸을 한 번에 비운다 */
    public void detachFile() {
        this.fileYn = "N";
        this.filePath = null;
        this.fileName = null;
        this.fileOrgName = null;
        this.fileExt = null;
        this.fileSize = null;
        this.updatedAt = nowUtc();
    }

    /** 사진이 붙어 있는지 */
    public boolean hasFile() {
        return "Y".equals(fileYn);
    }

    /** DB 가 UTC 로 적으므로 여기서 넣는 값도 UTC 여야 한다 */
    private static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
