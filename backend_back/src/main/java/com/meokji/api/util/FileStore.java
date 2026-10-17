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
 *
 * <p>쓰는 곳 — 자유게시판의 사진 붙이기 · 떼기 · 받기. 후기(review)도
 * 구현되면 같은 것을 쓴다. folder 인자로 "free" · "review" 를 나눠 받는다.
 *
 * <p>DB 와 디스크가 한 트랜잭션에 묶이지 않는다는 점을 기억해야 한다.
 * 파일을 저장한 뒤 DB UPDATE 가 실패하면 파일만 남고, 반대 순서면
 * DB 에는 적혔는데 파일이 없다. 순서를 어떻게 둘지는 부르는 쪽
 * (FreePostServiceImpl)이 정한다.
 */
@Component
public class FileStore {

    /** 올릴 수 있는 확장자. 목록에 없으면 받지 않는다 */
    // 허용할 것만 적는 방식이다. "이것만 안 된다" 로 두면 생각하지 못한
    // 확장자가 통과한다 - .html 을 올려 스크립트를 심거나, .jsp 로
    // 서버에서 실행되게 할 수도 있다
    private static final List<String> ALLOWED = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");

    /** 한 장 최대 크기 (5MB). 화면도 같은 값으로 미리 막는다 */
    // 양쪽에서 막는 이유 — 화면만 막으면 요청을 직접 보내는 길로는
    // 통과한다. 서버만 막으면 사용자가 10MB 를 다 올린 뒤에 거절당한다
    //
    // 5L 로 적는 이유 — int 끼리 곱하면 int 로 계산된다. 지금 값은
    // 안 넘치지만, 나중에 숫자를 키울 때 조용히 틀어진다
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
        // 먼저 받을 수 있는 파일인지 본다. 안 되면 여기서 끊긴다 -
        // 폴더를 만들거나 파일을 쓰기 전에 걸러낸다
        String ext = checkAndGetExt(file);

        // Kst.today() 를 쓴다. LocalDate.now() 면 서버가 선 지역 날짜가 되어,
        // 새벽에 올린 파일이 전날 폴더로 들어간다
        LocalDate today = Kst.today();
        // %04d · %02d 로 자리를 맞춘다. "2026/9" 가 아니라 "2026/09" 여야
        // 폴더 목록이 이름순으로 제대로 늘어선다
        String datePath = String.format("%s/%04d/%02d", folder, today.getYear(), today.getMonthValue());
        // 하이픈을 떼는 이유 — 파일명이 짧아지고, 주소에 들어갈 때 보기 좋다
        String savedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;

        try {
            Path dir = root().resolve(datePath);
            // 폴더가 없으면 만든다. 달이 바뀌면 새 폴더가 필요하고,
            // 처음 배포한 서버에는 아무 폴더도 없다
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(savedName).toFile());
        } catch (IOException e) {
            // 500 이 아니라 503 을 주는 이유 — 디스크가 찼거나 권한이
            // 없는 경우다. 우리 코드가 틀린 것이 아니라 "지금은 안 된다" 는 뜻이고,
            // 문장도 다시 해보라고 알려준다
            throw ApiException.serviceUnavailable("사진을 저장하지 못했습니다. 잠시 후 다시 시도해주세요.");
        }

        // 배열로 둘을 돌려준다. 전용 클래스를 만들 수도 있지만, 바로
        // VO 의 두 칸(path · savedName)에 옮겨 담는 자리라 그 사이에만 쓰인다
        return new String[] { datePath, savedName };
    }

    /** 저장해둔 사진을 읽는다. 파일이 없으면 404 */
    //
    // 통째로 읽어 바이트 배열로 돌려준다. 5MB 가 한계라 메모리에
    // 올려도 괜찮다. 더 큰 파일을 받게 되면 스트림으로 흘려보내야 한다
    public byte[] read(String filePath, String fileName) {
        Path target = root().resolve(filePath).resolve(fileName);

        // DB 에는 적혀 있는데 파일이 없는 경우. 디스크가 날아갔거나,
        // 지우기가 반쯤 되었거나, 서버를 옮기면서 파일을 안 가져온 경우다
        if (!Files.exists(target)) {
            throw ApiException.notFound("사진을 찾을 수 없습니다.");
        }

        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            // 파일은 있는데 읽히지 않는 경우(권한 등). 404 와 나눠 쓴다 -
            // "없다" 와 "못 읽는다" 는 고칠 방법이 다르다
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
            // deleteIfExists 는 파일이 없어도 예외를 던지지 않는다.
            // delete 를 쓰면 없을 때 터져서 try 로 또 감싸야 한다
            Files.deleteIfExists(root().resolve(filePath).resolve(fileName));
        } catch (IOException e) {
            // 지우지 못해도 넘어간다. 남은 파일은 주소를 모르면 열리지 않는다
            //
            // UUID 이름이라 짐작해서 맞힐 수 없고, DB 의 FILE_ 칸도
            // 비워지므로 그 주소를 알려줄 곳이 없다
        }
    }

    /** 확장자. 점은 빼고 소문자로 ('JPG' -> 'jpg') */
    // 소문자로 맞추는 이유 — ALLOWED 목록과 견주고, MIME 타입도
    // 소문자로 가린다. "JPG" 를 그대로 두면 허용 목록에 안 걸려 막힌다
    public String extensionOf(String originalName) {
        if (originalName == null) {
            return "";
        }

        // lastIndexOf 다. "my.photo.jpg" 처럼 점이 여러 개면 맨 뒤를 본다
        int dot = originalName.lastIndexOf('.');
        // 점이 없거나("photo") 점으로 끝나면("photo.") 확장자가 없다.
        // 빈 글자를 돌려주면 ALLOWED 에 안 걸려 거절된다
        if (dot < 0 || dot == originalName.length() - 1) {
            return "";
        }
        return originalName.substring(dot + 1).toLowerCase();
    }

    /** 184320 -> "180 KB". 화면이 그대로 보여준다 */
    // 서버가 만들어 보내는 이유 — 화면마다 계산하면 "180 KB" 와 "0.18 MB" 가
    // 섞인다. 단위를 고르는 기준이 한 곳에 있어야 같은 모양으로 보인다
    public String sizeText(Long bytes) {
        // 크기를 못 구한 파일도 있다(size 가 null). 빈 글자가 아니라
        // null 을 주면 화면이 그 자리를 아예 안 그린다
        if (bytes == null) {
            return null;
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return (bytes / 1024) + " KB";
        }
        // MB 만 소수점 한 자리를 보여준다. 5MB 가 한계라 "1 MB" 와
        // "4 MB" 뿐이면 차이가 안 느껴진다
        //
        // 1024.0 으로 나누는 이유 — 1024 로 나누면 정수 나눗셈이 되어
        // 소수점이 버려진다
        return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
    }

    /** 내려줄 때 쓸 MIME 타입. 모르면 바이너리로 준다 */
    //
    // 왜 필요한가 — 바이트만 보내면 브라우저가 무슨 그림인지 몰라
    // img 태그에 그리지 못하고 내려받기로 처리한다
    //
    // jpg 와 jpeg 를 같이 보는 이유 — 확장자는 둘이지만 MIME 타입은
    // image/jpeg 하나다
    //
    // ALLOWED 에 있는 확장자만 저장되므로 마지막 줄까지 올 일은 거의 없다.
    // 그래도 두는 이유는 옛 행에 다른 확장자가 남아 있을 수 있어서다
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
    //
    // 확인과 확장자 꺼내기를 한 메서드로 묶은 이유 — 확인하면서 이미
    // 확장자를 구한다. 따로 두면 extensionOf 를 두 번 부르게 된다
    //
    // 확장자만 보고 내용은 보지 않는다. .jpg 로 이름만 바꾼 다른 파일도
    // 통과한다 - 다만 저장 폴더를 바깥에서 열지 않고, 내려줄 때도
    // 이미지 MIME 타입을 붙이므로 서버에서 실행될 일은 없다
    private String checkAndGetExt(MultipartFile file) {
        // isEmpty 는 "칸은 있지만 내용이 없는" 경우도 잡는다.
        // 파일을 안 고르고 보내면 크기 0 으로 온다
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("사진을 선택해주세요.");
        }

        if (file.getSize() > MAX_BYTES) {
            throw ApiException.badRequest("5MB 이하 사진만 올릴 수 있어요.");
        }

        String ext = extensionOf(file.getOriginalFilename());
        if (!ALLOWED.contains(ext)) {
            // 쓸 수 있는 확장자를 문장에 적어준다. 안 적으면 사용자가
            // 무엇으로 바꿔야 할지 모른 채 다른 파일을 또 골라본다
            throw ApiException.badRequest("jpg · png · gif · webp 사진만 올릴 수 있어요.");
        }

        return ext;
    }
}
