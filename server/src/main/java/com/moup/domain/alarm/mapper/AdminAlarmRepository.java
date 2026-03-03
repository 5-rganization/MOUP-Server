package com.moup.domain.alarm.mapper;

import com.moup.domain.alarm.domain.AdminAlarm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAlarmRepository extends JpaRepository<AdminAlarm, Long> {

}
