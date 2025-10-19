-- 사용자 데이터 삽입 (1번 사용자는 alice, ROLE_ADMIN으로 설정)
INSERT INTO `users` (`id`, `provider`, `provider_id`, `username`, `nickname`, `role`, `profile_img`, `fcm_token`)
VALUES (1, 'LOGIN_GOOGLE', '100000000000000000001', 'alice', '앨리스', 'ROLE_ADMIN', 'http://example.com/profiles/alice.jpg',
        'fcm_token_for_alice_...'),
       (2, 'LOGIN_KAKAO', '2000000001', 'bob', '밥사장', 'ROLE_OWNER', 'http://example.com/profiles/bob.jpg',
        'fcm_token_for_bob_...'),
       (3, 'LOGIN_NAVER', '3000000001', 'charlie', '찰리', 'ROLE_WORKER', 'http://example.com/profiles/charlie.jpg',
        'fcm_token_for_charlie_...'),
       (4, 'LOGIN_APPLE', '4000000001.abc.123', 'diana', '디아나', 'ROLE_WORKER', 'http://example.com/profiles/diana.jpg',
        'fcm_token_for_diana_...'),
       (5, 'LOGIN_GOOGLE', '100000000000000000002', 'eve', '이브사장', 'ROLE_OWNER', 'http://example.com/profiles/eve.jpg',
        'fcm_token_for_eve_...');

-- 토큰 데이터 삽입
INSERT INTO `social_tokens` (`user_id`, `refresh_token`, `updated_at`)
VALUES (1, 'google_refresh_token_1', NOW()),
       (2, 'kakao_refresh_token_2', NOW()),
       (3, 'naver_refresh_token_3', NOW());

INSERT INTO `user_tokens` (`user_id`, `refresh_token`, `expiry_date`)
VALUES (1, 'app_refresh_token_1', DATE_ADD(NOW(), INTERVAL 14 DAY)),
       (2, 'app_refresh_token_2', DATE_ADD(NOW(), INTERVAL 14 DAY)),
       (3, 'app_refresh_token_3', DATE_ADD(NOW(), INTERVAL 14 DAY)),
       (4, 'app_refresh_token_4', DATE_ADD(NOW(), INTERVAL 14 DAY)),
       (5, 'app_refresh_token_5', DATE_ADD(NOW(), INTERVAL 14 DAY));

-- 루틴 및 루틴 작업 데이터 삽입
INSERT INTO `routines` (`user_id`, `routine_name`, `alarm_time`)
VALUES (3, '오픈 준비 루틴', '08:30:00'),
       (4, '마감 정리 루틴', '21:00:00');

INSERT INTO `routine_tasks` (`routine_id`, `content`, `order_index`)
VALUES (1, '커피 머신 예열', 1),
       (1, '포스기 켜기', 2),
       (1, '원두 재고 확인', 3),
       (2, '테이블 정리', 1),
       (2, '설거지 및 주방 마감', 2),
       (2, '전기 차단 및 소등', 3);

-- 알림 데이터 삽입
INSERT INTO `normal_alarms` (`sender_id`, `receiver_id`, `title`, `content`, `sent_at`)
VALUES (2, 3, '근무 시간 변경 요청', '찰리님, 다음 주 화요일 근무 시간을 오전 10시로 변경 가능할까요?', NOW());

INSERT INTO `admin_alarms` (`title`, `content`)
VALUES ('서비스 점검 안내', '10월 25일 오전 2시부터 4시까지 정기 점검이 있을 예정입니다.');

INSERT INTO `admin_alarm_user_mappings` (`alarm_id`, `user_id`)
VALUES (1, 2),
       (1, 3),
       (1, 4),
       (1, 5);

-- 근무지 데이터 삽입
INSERT INTO `workplaces` (`owner_id`, `workplace_name`, `category_name`, `is_shared`, `address`, `latitude`,
                          `longitude`)
VALUES (2, '밥스 카페', '카페', 1, '서울시 강남구 테헤란로 123', 37.50449, 127.0489),
       (5, '이브의 서점', '서점', 0, '경기도 성남시 분당구 판교역로 456', 37.3947, 127.1112);

-- 근로자 데이터 삽입 (근무지에 소속된 사용자)
INSERT INTO `workers` (`user_id`, `workplace_id`, `worker_based_label_color`, `owner_based_label_color`,
                       `is_accepted`)
VALUES (3, 1, '#FF5733', '#3375FF', 1),
       (4, 1, '#33FF57', '#FF33A1', 1),
       (4, 2, '#C70039', '#FFC300', 0); -- 디아나는 2번 근무지에도 초대받았지만 아직 수락 안 함

-- 급여 정보 데이터 삽입
INSERT INTO `salaries` (`worker_id`, `salary_type`, `salary_calculation`, `hourly_rate`, `salary_date`, `has_national_pension`, `has_health_insurance`, `has_employment_insurance`, `has_industrial_accident`, `has_income_tax`, `has_night_allowance`)
VALUES (1, 'SALARY_MONTHLY', 'SALARY_CALCULATION_HOURLY', 10000, 10, 1, 1, 1, 1, 1, 1),
       (2, 'SALARY_MONTHLY', 'SALARY_CALCULATION_FIXED', 2500000, 25, 1, 1, 1, 1, 1, 0);

-- 근무 기록 데이터 삽입
INSERT INTO `works` (`worker_id`, `work_date`, `start_time`, `actual_start_time`, `end_time`, `actual_end_time`,
                     `rest_time`, `memo`, `daily_income`, `hourly_rate`)
VALUES (1, '2025-10-18', '2025-10-18 09:00:00', '2025-10-18 08:58:00', '2025-10-18 18:00:00', '2025-10-18 18:05:00',
        60, '주말이라 바빴음', 80000, 10000),
       (1, '2025-10-19', '2025-10-19 09:00:00', '2025-10-19 09:01:00', '2025-10-19 18:00:00', '2025-10-19 18:00:00',
        60, NULL, 80000, 10000),
       (2, '2025-10-18', '2025-10-18 13:00:00', '2025-10-18 12:55:00', '2025-10-18 22:00:00', '2025-10-18 22:10:00',
        60, '마감 정리 추가 시간 발생', 85000, 9860);

-- 근무와 루틴 매핑 데이터 삽입
INSERT INTO `work_routine_mappings` (`work_id`, `routine_id`)
VALUES (1, 1), -- 1번 근무는 오픈 준비 루틴(1번)과 연결
       (3, 2);  -- 3번 근무는 마감 정리 루틴(2번)과 연결
