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
 * VO 하나가 DB 한 행 · 화면이 보낸 요청 · 화면으로 나갈 응답을 겸한다.
 * 나가면 안 되는 칸에는 @JsonIgnore 가 붙어 있다.
 *
 * 기능을 가리지 않고 같이 쓰는 것
 *   util       : 날씨 · 시세 API, 날짜 계산 등
 *   common     : 에러 처리 등 공통
 *   config     : 서버 설정 (CORS, 비밀번호 암호화)
 */
@SpringBootApplication
public class MeokjiApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeokjiApiApplication.class, args);
    }
}
