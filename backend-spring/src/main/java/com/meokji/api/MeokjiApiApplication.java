package com.meokji.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 뭐먹지? API 서버의 시작점. 이 main 을 실행하면 서버가 8080 포트로 뜬다.
 *
 * 폴더 구조 (요청이 들어오는 순서)
 *   controller : 주소(URL)를 받는 곳          예) BanController
 *   service    : 실제 일을 하는 곳 (인터페이스) 예) BanService
 *                + 그 구현                      예) BanServiceImpl
 *   dao        : DB 에 SQL 을 보내는 곳         예) BanDAO
 *   resources/mapper/*.xml : 실제 SQL           예) BanMapper.xml
 *   vo         : DB 테이블 한 행                예) UserBanVO
 *   dto        : 화면과 주고받는 요청 · 응답 모양  예) BanCreateRequest, BanResponse
 *   util       : 여러 곳에서 쓰는 도구 (날씨 · 시세 API, 날짜 계산 등)
 *   common     : 에러 처리 등 공통
 *   config     : 서버 설정 (CORS, 비밀번호 암호화)
 */
@SpringBootApplication
public class MeokjiApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeokjiApiApplication.class, args);
    }
}
