package com.meokji.api.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 비밀번호 재설정 인증번호를 메일로 보낸다.
 *
 * 발송 실패로 요청을 깨뜨리지 않는다
 *
 * <p>왜 깨뜨리지 않나 — 메일은 바깥 서비스다. 그쪽이 느리거나 막혔다고
 * 비밀번호 재설정 자체가 500 으로 끝나면, 사용자는 다시 눌러보는 수밖에
 * 없는데 그때는 재발송 제한(60초)에 걸린다. 코드는 이미 DB 에 저장돼
 * 있으므로, 메일만 못 간 상태로 두고 로그에 남긴다.
 *
 * <p>mock 과 smtp 두 가지로 쓰는 이유 — 개발하면서 메일 서버를 붙이려면
 * 계정과 앱 비밀번호가 필요하다. 팀원마다 그걸 준비하지 않으면 비밀번호
 * 재설정 화면을 끝까지 볼 수 없다. mock 이면 콘솔에 번호가 찍혀서
 * 그대로 입력해볼 수 있다.
 */
@Component
public class ResetCodeMailer {

    // 메일 제목. 앞에 서비스 이름을 붙여 받은편지함에서 가려지게 한다
    private static final String SUBJECT = "[뭐먹지?] 비밀번호 재설정 인증번호";

    // mock 이면 콘솔에 찍기만 하고, smtp 면 진짜 메일을 보낸다 (application.yml 의 app.mail.provider)
    @Value("${app.mail.provider}")
    private String mailProvider;

    // 보내는 사람 주소. 비워두면 spring.mail.username 이 쓰인다
    @Value("${app.mail.from}")
    private String mailFrom;

    /**
     * spring.mail.host 를 적지 않으면 스프링이 이 빈을 아예 만들지 않는다.
     * 그래서 직접 주입받지 않고 ObjectProvider 로 "있으면 쓴다" 로 둔다 -
     * mock 으로 쓰는 사람이 메일 설정 때문에 서버를 못 띄우면 안 된다.
     */
    @Autowired
    private ObjectProvider<JavaMailSender> mailSender;

    /**
     * 인증번호를 보낸다. provider 가 smtp 가 아니면 콘솔에 찍는다.
     *
     * 돌려주는 값이 없다. 보냈는지 못 보냈는지를 알려주지 않는 이유 —
     * 부르는 쪽(PasswordResetServiceImpl)이 그 결과로 할 일이 없다.
     * 실패했다고 코드를 지우면 사용자가 아까 받은 메일로도 못 들어간다
     */
    public void send(String to, String code, int expireMinutes) {
        if ("smtp".equalsIgnoreCase(mailProvider)) {
            sendBySmtp(to, code, expireMinutes);
            return;
        }

        printToConsole(to, code, expireMinutes);
    }

    /** 개발용. 메일 서버 없이 재설정 화면을 끝까지 볼 수 있다. */
    // 눈에 띄게 테두리를 둘러 찍는다. 서버 로그가 계속 흐르는 중에
    // 여섯 자리 숫자만 찍히면 찾기 어렵다
    //
    // 따옴표 세 개짜리 글자(텍스트 블록)를 쓴다. 줄마다 더하기로 잇는
    // 것보다 실제로 찍히는 모양이 코드에 그대로 보인다
    private void printToConsole(String to, String code, int expireMinutes) {
        System.out.println("""

                ===================== [MOCK EMAIL] =====================
                 받는 사람 : %s
                 인증번호  : %s
                 유효시간  : %d분
                  (실제로 발송되지 않았습니다. app.mail.provider=mock)
                ========================================================
                """.formatted(to, code, expireMinutes));
    }

    private void sendBySmtp(String to, String code, int expireMinutes) {
        // getIfAvailable 은 빈이 없으면 null 을 준다. 직접 주입받았다면
        // 빈이 없을 때 서버가 아예 안 떴을 것이다
        JavaMailSender sender = mailSender.getIfAvailable();

        if (sender == null) {
            // 설정이 덜 채워졌을 때 여기서 알려준다. 이걸 안 하면 "메일이 안 온다"
            // 는 증상만 보이고 원인을 찾기 어렵다.
            System.out.println("[SMTP 설정 오류] spring.mail.host 가 비어 있어 메일을 "
                    + "보내지 못했습니다. application-local.yml 을 확인하세요.");
            // 설정이 틀렸어도 콘솔에는 찍어준다. 개발 중이라면 그 번호로
            // 이어서 확인할 수 있다
            printToConsole(to, code, expireMinutes);
            return;
        }

        // SimpleMailMessage 는 글자만 보내는 메일이다. HTML 로 꾸미려면
        // MimeMessage 를 써야 하는데, 인증번호 한 줄에 그럴 이유가 없다
        SimpleMailMessage message = new SimpleMailMessage();

        // 발신자를 비워두면 spring.mail.username 이 쓰인다. 대부분의 메일 서버는
        // 로그인 계정과 다른 발신자를 거부한다.
        if (!mailFrom.isBlank()) {
            message.setFrom(mailFrom);
        }

        message.setTo(to);
        message.setSubject(SUBJECT);
        message.setText("""
                안녕하세요, 뭐먹지? 입니다.

                비밀번호 재설정 인증번호는 [ %s ] 입니다.
                %d분 안에 입력해주세요.

                본인이 요청한 것이 아니라면 이 메일을 무시하셔도 됩니다.
                누군가 회원님의 이메일 주소를 잘못 입력했을 수 있습니다.
                """.formatted(code, expireMinutes));

        try {
            sender.send(message);
        } catch (MailException exc) {
            // 예외를 올려보내지 않고 로그만 남긴다 (클래스 주석 참고).
            // 메일이 안 갔다는 사실은 서버 로그에서 확인할 수 있어야 한다
            System.out.println("[SMTP 발송 실패] " + exc.getClass().getSimpleName()
                    + ": " + exc.getMessage());
        }
    }
}
