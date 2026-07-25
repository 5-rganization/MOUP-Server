package com.moup.domain.alarm.application;

import com.moup.domain.alarm.domain.AnnouncementCreatedEvent;
import com.moup.domain.alarm.mapper.AdminAlarmUserMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnnouncementCreatedEventHandler {

  private final AdminAlarmUserMappingRepository mappingRepository;

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void createMappings(AnnouncementCreatedEvent event) {
    int createdCount = mappingRepository.createMappingsForAllUsers(event.announcementId());
    log.info(
        "Created {} announcement mappings for announcementId={}",
        createdCount,
        event.announcementId()
    );
  }
}
