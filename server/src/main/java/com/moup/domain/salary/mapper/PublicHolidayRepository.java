package com.moup.domain.salary.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/// 관공서 공휴일 (`public_holidays`). 휴일근로 가산이 쓴다(Phase 11 Q6).
@Mapper
public interface PublicHolidayRepository {

    /// `from`~`to`(양 끝 포함) 사이의 공휴일 날짜.
    @Select("SELECT holiday_date FROM public_holidays WHERE holiday_date BETWEEN #{from} AND #{to}")
    List<LocalDate> findDatesBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /// 그해 공휴일이 하나라도 등록돼 있는지. 없으면 표를 갱신하지 않은 것이다.
    @Select("SELECT EXISTS(SELECT 1 FROM public_holidays WHERE YEAR(holiday_date) = #{year})")
    boolean existsInYear(@Param("year") int year);
}
