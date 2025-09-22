-- moup 스키마 사용
USE moup;

-- users 테이블 더미 데이터 (2명의 사장, 3명의 직원)
INSERT INTO `users` (`provider`, `provider_id`, `username`, `nickname`, `role`, `profile_img`)
VALUES ('LOGIN_GOOGLE', '100000000000000000001', '김사장', '사장님1', 'ROLE_OWNER', 'https://example.com/profile1.jpg'),
       ('LOGIN_APPLE', '000001.a1b2c3d4e5f6.0001', '박사장', '사장님2', 'ROLE_OWNER', 'https://example.com/profile2.jpg'),
       ('LOGIN_GOOGLE', '100000000000000000002', '최알바', '성실알바', 'ROLE_WORKER', 'https://example.com/profile3.jpg'),
       ('LOGIN_GOOGLE', '100000000000000000003', '이알바', '주말알바', 'ROLE_WORKER', 'https://example.com/profile4.jpg'),
       ('LOGIN_APPLE', '000002.f6e5d4c3b2a1.0002', '강알바', '미소알바', 'ROLE_WORKER', 'https://example.com/profile5.jpg');

-- social_tokens 테이블 더미 데이터
INSERT INTO `social_tokens` (`user_id`, `refresh_token`, `updated_at`)
VALUES (1, 'google_refresh_token_string_example_1', NOW()),
       (2, 'apple_refresh_token_string_example_2', NOW()),
       (3, 'google_refresh_token_string_example_3', NOW()),
       (4, 'google_refresh_token_string_example_4', NOW()),
       (5, 'apple_refresh_token_string_example_5', NOW());

-- user_tokens 테이블 더미 데이터
INSERT INTO `user_tokens` (`user_id`, `refresh_token`, `expiry_date`)
VALUES (1, 'jwt_refresh_token_example_user1', DATE_ADD(NOW(), INTERVAL 14 DAY)),
       (2, 'jwt_refresh_token_example_user2', DATE_ADD(NOW(), INTERVAL 14 DAY)),
       (3, 'jwt_refresh_token_example_user3', DATE_ADD(NOW(), INTERVAL 14 DAY)),
       (4, 'jwt_refresh_token_example_user4', DATE_ADD(NOW(), INTERVAL 14 DAY)),
       (5, 'jwt_refresh_token_example_user5', DATE_ADD(NOW(), INTERVAL 14 DAY));

-- routines 테이블 더미 데이터
INSERT INTO `routines` (`user_id`, `routine_name`, `alarm_time`)
VALUES (3, '오전 오픈 준비', '08:30:00'),
       (3, '오후 마감 정리', '22:00:00'),
       (4, '주말 오픈 루틴', '10:00:00'),
       (1, '매니저 확인 사항', '09:00:00'),
       (5, '청결 관리 루틴', '15:00:00');

-- routine_tasks 테이블 더미 데이터
INSERT INTO `routine_tasks` (`routine_id`, `user_id`, `content`, `order_index`)
VALUES (1, 3, '포스기 켜고 시재 확인', 1),
       (1, 3, '매장 조명 켜기', 2),
       (2, 3, '쓰레기통 비우기', 1),
       (2, 3, '커피 머신 세척', 2),
       (3, 4, '유통기한 확인 및 폐기 등록', 1);

-- alarms 테이블 더미 데이터
INSERT INTO `alarms` (`sender_id`, `receiver_id`, `title`, `content`, `sent_at`, `alarm_type`)
VALUES (1, 3, '근무 시간 변경 요청', '내일 1시간 일찍 출근 가능할까요?', NOW(), 'ALARM_NOTIFICATION'),
       (2, 4, '급여 지급 완료', '9월 급여가 지급되었습니다. 확인해주세요.', NOW(), 'ALARM_NOTIFICATION'),
       (3, 1, '업무 관련 문의', '신제품 재고가 부족합니다.', NOW(), 'ALARM_NOTIFICATION'),
       (4, 2, '대타 근무 가능 문의', '다음 주 화요일 대타 가능하신 분 찾습니다.', NOW(), 'ALARM_NOTIFICATION'),
       (1, 5, '공지사항', '이번 주말 워크샵 관련 공지입니다.', NOW(), 'ALARM_NOTIFICATION');

-- workplaces 테이블 더미 데이터
INSERT INTO `workplaces` (`owner_id`, `workplace_name`, `category_name`, `label_color`, `is_shared`, `address`)
VALUES (1, 'GS25 역삼점', '편의점', 'blue', 1, '서울 강남구 역삼동 123-45'),
       (1, '메가커피 선릉점', '카페', 'green', 0, '서울 강남구 대치동 678-90'),
       (2, '홍콩반점 홍대입구역점', '음식점', 'red', 1, '서울 마포구 서교동 345-67'),
       (2, '올리브영 신촌점', '판매점', 'purple', 0, '서울 서대문구 창천동 543-21'),
       (1, '개인 스터디 카페', '카페', 'orange', 0, '서울 관악구 신림동 111-22');

-- workers 테이블 더미 데이터 (3명의 직원을 근무지에 배정)
INSERT INTO `workers` (`user_id`, `workplace_id`)
VALUES (3, 1), -- 최알바 -> GS25 역삼점
       (4, 3), -- 이알바 -> 홍콩반점 홍대입구역점
       (5, 2), -- 강알바 -> 메가커피 선릉점
       (3, 2), -- 최알바 -> 메가커피 선릉점 (한명이 여러곳에서 근무)
       (4, 4); -- 이알바 -> 올리브영 신촌점

-- salaries 테이블 더미 데이터
-- 주의: worker_id는 위 workers 테이블에 삽입된 후 생성된 ID를 참조해야 합니다. (자동 증가 값이 1, 2, 3, 4, 5라고 가정)
INSERT INTO `salaries` (`worker_id`, `salary_type`, `salary_calculation`, `hourly_rate`, `fixed_rate`,
                        `salary_date`, `salary_day`, `has_national_pension`, `has_health_insurance`,
                        `has_employment_insurance`, `has_industrial_accident`, `has_income_tax`, `has_night_allowance`)
VALUES (1, 'SALARY_MONTHLY', 'SALARY_CALCULATION_HOURLY', 10000, NULL, 10, NULL, 1, 1, 1, 1, 1, 1),
       (2, 'SALARY_WEEKLY', 'SALARY_CALCULATION_HOURLY', 11000, NULL, NULL, 'MON', 0, 0, 1, 1, 1, 1),
       (3, 'SALARY_MONTHLY', 'SALARY_CALCULATION_FIXED', NULL, 2300000, 25, NULL, 1, 1, 1, 1, 1, 0),
       (4, 'SALARY_DAILY', 'SALARY_CALCULATION_HOURLY', 9860, NULL, NULL, NULL, 0, 0, 0, 1, 1, 0),
       (5, 'SALARY_MONTHLY', 'SALARY_CALCULATION_HOURLY', 12000, NULL, 15, NULL, 1, 1, 1, 1, 1, 1);

-- works 테이블 더미 데이터
-- 주의: worker_id와 routine_id는 위 테이블들에서 생성된 ID를 참조해야 합니다.
INSERT INTO `works` (`worker_id`, `routine_id`, `work_date`, `start_time`, `actual_start_time`, `end_time`,
                     `actual_end_time`, `rest_time`, `memo`, `daily_income`)
VALUES (1, 1, '2025-09-15', '09:00:00', '08:58:00', '18:00:00', '18:03:00', '01:00:00', '월요일 오픈 근무', 80000),
       (2, NULL, '2025-09-16', '14:00:00', '14:05:00', '22:00:00', '22:10:00', '01:00:00', '재고 정리', 77000),
       (3, NULL, '2025-09-17', '10:00:00', '10:00:00', '19:00:00', '19:00:00', '01:00:00', NULL, 100000),
       (1, 2, '2025-09-18', '18:00:00', '17:55:00', '23:00:00', '23:00:00', '00:30:00', '마감 근무', 45000),
       (4, NULL, '2025-09-19', '09:00:00', '09:02:00', '15:00:00', '15:00:00', '00:30:00', '오전 파트타임', 54230);
