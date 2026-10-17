package com.meokji.api.health.dao;

import org.apache.ibatis.annotations.Mapper;

/**
 * DB 연결 확인. SQL 은 resources/mapper/HealthMapper.xml 에 있다.
 *
 * <p>SQL 한 줄을 XML 에 따로 두는 것이 번거로워 보이지만, 다른 DAO 와 같은
 * 모양을 지키기 위한 것이다. 여기만 {@code @Select("...")} 로 자바에 적으면
 * SQL 이 어디 있는지가 기능마다 달라진다.
 */
@Mapper
public interface HealthDAO {

    /**
     * SELECT 1 FROM DUAL. DB 가 안 되면 에러가 난다
     *
     * 돌려받는 1 은 쓰지 않는다. 중요한 것은 "에러 없이 돌아왔는가" 다.
     * 서비스는 이 값을 받지도 않고 try-catch 로 감싸기만 한다.
     *
     * DUAL 은 오라클이 기본으로 주는 한 행짜리 가짜 표다. 표를 안 읽고
     * 연결만 확인하려는 자리에서 쓴다 - 우리 표를 읽으면 그 표가 없거나
     * 권한이 없을 때도 "DB 가 죽었다" 로 보인다.
     */
    int ping();
}
