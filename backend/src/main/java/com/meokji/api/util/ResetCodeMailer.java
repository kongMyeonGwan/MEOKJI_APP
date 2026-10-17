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
 */
@Component
public class ResetCodeMailer {

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

    /** 인증번호를 보낸다. provider 가 smtp 가 아니면 콘솔에 찍는다. */
    public void send(String to, String code, int expireMinutes) {
        if ("smtp".equalsIgnoreCase(mailProvider)) {
            sendBySmtp(to, code, expireMinutes);
            return;
        }

        printToConsole(to, code, expireMinutes);
    }

    /** 개발용. 메일 서버 없이 재설정 화면을 끝까지 볼 수 있다. */
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
        JavaMailSender sender = mailSender.getIfAvailable();

        if (sender == null) {
            // 설정이 덜 채워졌을 때 여기서 알려준다. 이걸 안 하면 "메일이 안 온다"
            // 는 증상만 보이고 원인을 찾기 어렵다.
            System.out.println("[SMTP 설정 오류] spring.mail.host 가 비어 있어 메일을 "
                    + "보내지 못했습니다. application-local.yml 을 확인하세요.");
            printToConsole(to, code, expireMinutes);
            return;
        }

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
            System.out.println("[SMTP 발송 실패] " + exc.getClass().getSimpleName()
                    + ": " + exc.getMessage());
        }
    }
}
