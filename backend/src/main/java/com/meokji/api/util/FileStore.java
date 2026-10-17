package com.meokji.api.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.meokji.api.common.ApiException;

/**
 * 올린 사진을 디스크에 저장하고 읽고 지운다.
 *
 * <p>왜 DB 에 넣지 않나
 *
 * <p>BLOB 으로 넣으면 목록을 읽을 때마다 사진이 딸려 나와 게시판이 급격히
 * 느려지고, 백업 용량이 사진 용량만큼 그대로 불어난다. 그래서 파일은
 * 디스크에 두고 표에는 경로만 적는다(FREE_POSTS 의 FILE_* 칸).
 *
 * <p>저장 위치
 *
 * <pre>
 * data/uploads/free/2026/10/3f2a....jpg
 *              └ 폴더(filePath)  └ 저장 이름(fileName)
 * </pre>
 *
 * <p>날짜로 폴더를 나누는 이유는 한 폴더에 파일이 수만 개 쌓이면 운영체제가
 * 목록을 읽는 것부터 느려지기 때문이다.
 *
 * <p>이름을 UUID 로 바꾸는 이유
 *
 * <p>올린 이름을 그대로 쓰면 같은 이름끼리 서로 덮어쓴다. 그리고 이름에
 * {@code ../../.env} 같은 경로가 섞여 들어오면 저장 위치를 벗어나 서버 파일을
 * 건드릴 수 있다(경로 조작). 원래 이름은 표의 FILE_ORG_NAME 에만 남긴다.
 */
@Component
public class FileStore {

    /** 올릴 수 있는 확장자. 목록에 없으면 받지 않는다 */
    private static final List<String> ALLOWED = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");

    /** 한 장 최대 크기 (5MB). 화면도 같은 값으로 미리 막는다 */
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    /** 저장 폴더. application.yml 의 app.upload-dir */
    @Value("${app.upload-dir}")
    private String uploadDir;

    /**
     * 저장 폴더를 절대 경로로 바꿔서 돌려준다.
     *
     * <p>설정값은 'data/uploads' 같은 상대 경로라 서버를 켠 자리에 따라 가리키는
     * 곳이 달라진다. 특히 MultipartFile.transferTo 는 상대 경로를 받으면 우리가
     * 생각하는 자리가 아니라 <b>톰캣의 업로드 임시 폴더</b> 기준으로 푼다. 그래서
     * 폴더는 멀쩡히 만들어졌는데 파일만 안 써지는 일이 생긴다(503).
     * 읽기 · 지우기도 같은 기준이어야 하므로 한 곳에서 절대 경로로 맞춘다.
     */
    private Path root() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * 사진을 저장하고 "폴더/저장이름" 두 조각을 돌려준다.
     *
     * @param folder 게시판 이름 같은 맨 앞 칸 (예: "free")
     * @return [0] = 폴더 경로(free/2026/10), [1] = 저장 이름(3f2a....jpg)
     */
    public String[] save(MultipartFile file, String folder) {
        String ext = checkAndGetExt(file);

        LocalDate today = Kst.today();
        String datePath = String.format("%s/%04d/%02d", folder, today.getYear(), today.getMonthValue());
        String savedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;

        try {
            Path dir = root().resolve(datePath);
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(savedName).toFile());
        } catch (IOException e) {
            throw ApiException.serviceUnavailable("사진을 저장하지 못했습니다. 잠시 후 다시 시도해주세요.");
        }

        return new String[] { datePath, savedName };
    }

    /** 저장해둔 사진을 읽는다. 파일이 없으면 404 */
    public byte[] read(String filePath, String fileName) {
        Path target = root().resolve(filePath).resolve(fileName);

        if (!Files.exists(target)) {
            throw ApiException.notFound("사진을 찾을 수 없습니다.");
        }

        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw ApiException.serviceUnavailable("사진을 읽지 못했습니다.");
        }
    }

    /**
     * 저장해둔 사진을 지운다.
     *
     * <p>파일이 이미 없어도 에러로 보지 않는다. 지우는 쪽에서 중요한 것은
     * 표의 FILE_* 칸을 비우는 일이고, 파일이 먼저 사라졌다고 해서 글 수정이
     * 막히면 사용자는 영문을 모른 채 막힌다.
     */
    public void delete(String filePath, String fileName) {
        if (filePath == null || fileName == null) {
            return;
        }

        try {
            Files.deleteIfExists(root().resolve(filePath).resolve(fileName));
        } catch (IOException e) {
            // 지우지 못해도 넘어간다. 남은 파일은 주소를 모르면 열리지 않는다
        }
    }

    /** 확장자. 점은 빼고 소문자로 ('JPG' -> 'jpg') */
    public String extensionOf(String originalName) {
        if (originalName == null) {
            return "";
        }

        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) {
            return "";
        }
        return originalName.substring(dot + 1).toLowerCase();
    }

    /** 184320 -> "180 KB". 화면이 그대로 보여준다 */
    public String sizeText(Long bytes) {
        if (bytes == null) {
            return null;
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return (bytes / 1024) + " KB";
        }
        return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
    }

    /** 내려줄 때 쓸 MIME 타입. 모르면 바이너리로 준다 */
    public String contentType(String ext) {
        if ("png".equals(ext)) {
            return "image/png";
        }
        if ("gif".equals(ext)) {
            return "image/gif";
        }
        if ("webp".equals(ext)) {
            return "image/webp";
        }
        if ("jpg".equals(ext) || "jpeg".equals(ext)) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }

    // 받을 수 있는 파일인지 보고, 괜찮으면 확장자를 돌려준다
    private String checkAndGetExt(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("사진을 선택해주세요.");
        }

        if (file.getSize() > MAX_BYTES) {
            throw ApiException.badRequest("5MB 이하 사진만 올릴 수 있어요.");
        }

        String ext = extensionOf(file.getOriginalFilename());
        if (!ALLOWED.contains(ext)) {
            throw ApiException.badRequest("jpg · png · gif · webp 사진만 올릴 수 있어요.");
        }

        return ext;
    }
}
