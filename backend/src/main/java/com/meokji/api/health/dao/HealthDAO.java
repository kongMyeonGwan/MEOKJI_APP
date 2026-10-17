package com.meokji.api.health.dao;

import org.apache.ibatis.annotations.Mapper;

/** DB 연결 확인. SQL 은 resources/mapper/HealthMapper.xml 에 있다. */
@Mapper
public interface HealthDAO {

    // SELECT 1 FROM DUAL. DB 가 안 되면 에러가 난다
    int ping();
}
