package com.meokji.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 뭐먹지? API 서버의 시작점. 이 main 을 실행하면 서버가 8090 포트로 뜬다.
 *
 * 폴더는 기능(화면)별로 먼저 나뉘고, 그 아래가 계층이다.
 * 예) ban/  = 못 먹는 재료 기능 한 벌
 *
 *   <기능>/controller : 주소(URL)를 받는 곳          예) BanController
 *   <기능>/service    : 실제 일을 하는 곳 (인터페이스) 예) BanService
 *                       + 그 구현                      예) BanServiceImpl
 *   <기능>/dao        : DB 에 SQL 을 보내는 곳         예) BanDAO
 *   <기능>/vo         : 값을 담는 상자                 예) UserBanVO
 *   resources/mapper/*.xml : 실제 SQL                  예) BanMapper.xml
 *
 * 계층별로 먼저 나누지 않은 이유 — controller/ 아래에 20개, service/ 아래에
 * 20개가 쌓이면 한 기능을 고칠 때 폴더 네 개를 왕복해야 한다. 기능으로 먼저
 * 나누면 "못 먹는 재료" 를 고칠 때 ban/ 폴더만 열면 된다.
 *
 * VO 하나가 DB 한 행 · 화면이 보낸 요청 · 화면으로 나갈 응답을 겸한다.
 * 나가면 안 되는 칸에는 @JsonIgnore 가 붙어 있다.
 * (요청용 · 응답용 · DB용을 따로 만들면 칸 세 개를 같이 고쳐야 한다)
 *
 * 기능을 가리지 않고 같이 쓰는 것
 *   util       : 날씨 · 시세 API, 날짜 계산 등
 *   common     : 에러 처리 등 공통
 *   config     : 서버 설정 (CORS, 비밀번호 암호화)
 *
 * 한 가지 예외가 있다 — catalog 기능에는 dao 와 vo 가 없다. 카테고리 · 룰렛처럼
 * 아무도 고치지 않는 고정 목록은 표가 아니라 resources/data 의 JSON 파일에서
 * 읽기 때문이다. free 기능에는 반대로 entity · repository 가 더 있다(JPA 로 옮긴 부분).
 *
 * @SpringBootApplication 하나가 세 가지를 한다
 *   - 이 패키지(com.meokji.api) 아래를 전부 훑어 @Service · @RestController 등을 찾는다
 *   - DB · 메일 · JSON 같은 기본 설정을 알아서 맞춘다
 *   - 내장 톰캣을 띄운다 (따로 서버를 설치하지 않아도 되는 이유)
 */
@SpringBootApplication
public class MeokjiApiApplication {

    // 서버를 띄우는 자리. 여기서 끝나지 않고 톰캣이 계속 떠 있으면서 요청을 기다린다.
    public static void main(String[] args) {
        SpringApplication.run(MeokjiApiApplication.class, args);
    }
}
