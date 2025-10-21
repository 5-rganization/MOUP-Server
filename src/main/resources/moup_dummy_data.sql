-- MOUP 더미데이터 (스키마 수정 및 관리자 규칙 반영)
USE moup;

-- 깨끗하게 초기화
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE work_routine_mappings;
TRUNCATE TABLE works;
TRUNCATE TABLE routine_tasks;
TRUNCATE TABLE routines;
TRUNCATE TABLE salaries;
TRUNCATE TABLE workers;
TRUNCATE TABLE workplaces;
TRUNCATE TABLE normal_alarms;
TRUNCATE TABLE admin_alarm_user_mappings;
TRUNCATE TABLE admin_alarms;
TRUNCATE TABLE social_tokens;
TRUNCATE TABLE user_tokens;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- =====================================
-- 1) USERS
-- =====================================
INSERT INTO users (provider, provider_id, username, nickname, role, profile_img, is_deleted, deleted_at, fcm_token) VALUES
                                                                                                                        ('LOGIN_GOOGLE', '100000000000000000001', '관리자', '관리자', 'ROLE_ADMIN', 'https://example.com/profile1.jpg', 0, NULL, 'fcm_token_admin'),
                                                                                                                        ('LOGIN_APPLE', '000001.a1b2c3d4e5f6.0001', '박사장', '홍대점주', 'ROLE_OWNER', 'https://example.com/profile2.jpg', 0, NULL, 'fcm_token_owner_park'),
                                                                                                                        ('LOGIN_GOOGLE', '100000000000000000002', '최알바', '성실알바최씨', 'ROLE_WORKER', 'https://example.com/profile3.jpg', 0, NULL, 'fcm_token_worker_choi'),
                                                                                                                        ('LOGIN_GOOGLE', '100000000000000000003', '이알바', '주말알바이씨', 'ROLE_WORKER', 'https://example.com/profile4.jpg', 0, NULL, NULL),
                                                                                                                        ('LOGIN_APPLE', '000002.f6e5d4c3b2a1.0002', '강알바', '미소알바강씨', 'ROLE_WORKER', 'https://example.com/profile5.jpg', 0, NULL, 'fcm_token_worker_kang');

-- 변수명 명확화 (관리자, 사장 구분)
SET @u_admin = 1;
SET @u_owner_park = 2;
SET @u_choi_worker = 3;
SET @u_lee_worker = 4;
SET @u_kang_worker = 5;

-- =====================================
-- 2) TOKENS
-- =====================================
INSERT INTO social_tokens (user_id, refresh_token, updated_at) VALUES
                                                                   (@u_admin, 'google_refresh_token_string_example_1', NOW()),
                                                                   (@u_owner_park, 'apple_refresh_token_string_example_2', NOW()),
                                                                   (@u_choi_worker, 'google_refresh_token_string_example_3', NOW()),
                                                                   (@u_lee_worker, 'google_refresh_token_string_example_4', NOW()),
                                                                   (@u_kang_worker, 'apple_refresh_token_string_example_5', NOW());

INSERT INTO user_tokens (user_id, refresh_token, expiry_date, created_at) VALUES
                                                                              (@u_admin, 'jwt_refresh_token_example_user1', DATE_ADD(NOW(), INTERVAL 14 DAY), NOW()),
                                                                              (@u_owner_park, 'jwt_refresh_token_example_user2', DATE_ADD(NOW(), INTERVAL 14 DAY), NOW()),
                                                                              (@u_choi_worker, 'jwt_refresh_token_example_user3', DATE_ADD(NOW(), INTERVAL 14 DAY), NOW()),
                                                                              (@u_lee_worker, 'jwt_refresh_token_example_user4', DATE_ADD(NOW(), INTERVAL 14 DAY), NOW()),
                                                                              (@u_kang_worker, 'jwt_refresh_token_example_user5', DATE_ADD(NOW(), INTERVAL 14 DAY), NOW());

-- =====================================
-- 3) WORKPLACES
-- =====================================
-- (수정) 관리자(@u_admin)가 소유한 근무지를 박사장(@u_owner_park)이 소유하도록 변경
INSERT INTO workplaces (owner_id, workplace_name, category_name, is_shared, address, latitude, longitude) VALUES
                                                                                                              (@u_owner_park, 'GS25 역삼점', '편의점', 1, '서울 강남구 역삼동 123-45', 37.5009, 127.0374),
                                                                                                              (@u_owner_park, '메가커피 선릉점', '카페', 1, '서울 강남구 대치동 678-90', 37.5042, 127.0488),
                                                                                                              (@u_owner_park, '홍콩반점 홍대입구역점', '음식점', 1, '서울 마포구 서교동 345-67', 37.5567, 126.9237),
                                                                                                              (@u_owner_park, '올리브영 신촌점', '판매점', 1, '서울 서대문구 창천동 543-21', 37.5598, 126.9423),
                                                                                                              (@u_owner_park, '개인 스터디 카페', '카페', 1, '서울 관악구 신림동 111-22', 37.4844, 126.9294);

SET @wp_gs25 = 1;
SET @wp_mega = 2;
SET @wp_hongkong = 3;
SET @wp_olive = 4;
SET @wp_studycafe = 5;

-- =====================================
-- 4) WORKERS (user↔workplace)
-- =====================================
-- (수정) 관리자(@u_admin)가 근무자로 등록된 레코드 삭제
INSERT INTO workers (user_id, workplace_id, worker_based_label_color, owner_based_label_color, is_accepted) VALUES
                                                                                                                (@u_choi_worker, @wp_gs25, '#FF4136', '#FF4136', 1),  -- 최알바 -> GS25 역삼점
                                                                                                                (@u_lee_worker, @wp_hongkong, '#2ECC40', '#2ECC40', 1), -- 이알바 -> 홍콩반점
                                                                                                                (@u_kang_worker, @wp_mega, '#0074D9', '#0074D9', 1),    -- 강알바 -> 메가커피
                                                                                                                (@u_choi_worker, @wp_mega, '#B10DC9', '#B10DC9', 1),    -- 최알바 -> 메가커피 (중복 근무)
                                                                                                                (@u_lee_worker, @wp_olive, '#FF851B', '#FF851B', 1),  -- 이알바 -> 올리브영 (중복 근무)
                                                                                                                (@u_owner_park, @wp_hongkong, '#FFDC00', '#FFDC00', 1);-- 박사장 -> 홍콩반점 (사장도 근무자로 등록)

SET @wk_choi_gs25 = 1;
SET @wk_lee_hongkong = 2;
SET @wk_kang_mega = 3;
SET @wk_choi_mega = 4;
SET @wk_lee_olive = 5;
-- SET @wk_kim_gs25 = 6; (삭제)
SET @wk_park_hongkong = 6; -- (ID 수정 7 -> 6)

-- =====================================
-- 5) ROUTINES & TASKS
-- =====================================
-- (수정) 관리자(@u_admin)의 루틴을 박사장(@u_owner_park)에게 재할당
INSERT INTO routines (user_id, routine_name, alarm_time) VALUES
                                                             (@u_choi_worker, '오전 오픈 준비', '08:30:00'),
                                                             (@u_choi_worker, '오후 마감 정리', '22:00:00'),
                                                             (@u_lee_worker, '주말 오픈 루틴', '10:00:00'),
                                                             (@u_owner_park, '매니저 확인 사항', '09:00:00'), -- (소유자 변경)
                                                             (@u_kang_worker, '청결 관리 루틴', '15:00:00');

SET @r_choi_open = 1;
SET @r_choi_close = 2;
SET @r_lee_open = 3;
SET @r_kim_check = 4;
SET @r_kang_clean = 5;

INSERT INTO routine_tasks (routine_id, content, order_index) VALUES
                                                                 (@r_choi_open, '포스기 켜고 시재 확인', 1),
                                                                 (@r_choi_open, '매장 조명 켜기', 2),
                                                                 (@r_choi_open, '원두 재고 확인', 3),
                                                                 (@r_choi_close, '쓰레기통 비우기', 1),
                                                                 (@r_choi_close, '커피 머신 세척', 2),
                                                                 (@r_lee_open, '유통기한 확인 및 폐기 등록', 1),
                                                                 (@r_kim_check, 'CCTV 확인', 1),
                                                                 (@r_kang_clean, '화장실 청소 상태 점검', 1);

-- =====================================
-- 6) SALARIES (급여 정책)
-- =====================================
INSERT INTO salaries (worker_id, salary_type, salary_calculation, hourly_rate, fixed_rate, salary_date, salary_day, has_national_pension, has_health_insurance, has_employment_insurance, has_industrial_accident, has_income_tax, has_night_allowance) VALUES
                                                                                                                                                                                                                                                            (@wk_choi_gs25, 'SALARY_MONTHLY', 'SALARY_CALCULATION_HOURLY', 10000, NULL, 10, NULL, 1, 1, 1, 1, 1, 1),
                                                                                                                                                                                                                                                            (@wk_lee_hongkong, 'SALARY_WEEKLY', 'SALARY_CALCULATION_HOURLY', 11000, NULL, NULL, 'MONDAY', 0, 0, 1, 1, 1, 1),
                                                                                                                                                                                                                                                            (@wk_kang_mega, 'SALARY_MONTHLY', 'SALARY_CALCULATION_FIXED', NULL, 2300000, 25, NULL, 1, 1, 1, 1, 1, 0),
                                                                                                                                                                                                                                                            (@wk_choi_mega, 'SALARY_DAILY', 'SALARY_CALCULATION_HOURLY', 9860, NULL, NULL, NULL, 0, 0, 0, 1, 1, 0),
                                                                                                                                                                                                                                                            (@wk_lee_olive, 'SALARY_MONTHLY', 'SALARY_CALCULATION_HOURLY', 12000, NULL, 15, NULL, 1, 1, 1, 1, 1, 1);

-- =====================================
-- 7) WORKS (근무 기록) - 스키마에 맞게 급여 필드 추가 및 재계산
-- =====================================
INSERT INTO works (
    worker_id, work_date, start_time, actual_start_time, end_time, actual_end_time,
    rest_time_minutes, gross_work_minutes, net_work_minutes, night_work_minutes,
    memo, hourly_rate, base_pay, night_allowance, holiday_allowance,
    gross_income, estimated_net_income, repeat_days, repeat_end_date
) VALUES
-- 1. 최알바 @ GS25: 9-18시 (야간 0분)
(@wk_choi_gs25, '2025-10-15', '2025-10-15 09:00:00', '2025-10-15 08:58:00', '2025-10-15 18:00:00', '2025-10-15 18:03:00', 60, 540, 480, 0, '월요일 오픈 근무', 10000, 80000, 0, 0, 80000, 77360, NULL, NULL),

-- 2. 이알바 @ 홍콩반점: 14-22시 (야간 0분)
(@wk_lee_hongkong, '2025-10-16', '2025-10-16 14:00:00', '2025-10-16 14:05:00', '2025-10-16 22:00:00', '2025-10-16 22:10:00', 60, 480, 420, 0, '재고 정리', 11000, 77000, 0, 0, 77000, 74490, NULL, NULL),

-- 3. 강알바 @ 메가커피: 10-19시 (야간 0분)
(@wk_kang_mega, '2025-10-17', '2025-10-17 10:00:00', '2025-10-17 10:00:00', '2025-10-17 19:00:00', '2025-10-17 19:00:00', 60, 540, 480, 0, NULL, NULL, 100000, 0, 0, 100000, 96700, NULL, NULL),

-- 4. 최알바 @ 메가커피: 18-23시 (야간 60분: 22:00~23:00)
(@wk_choi_mega, '2025-10-18', '2025-10-18 18:00:00', '2025-10-18 17:55:00', '2025-10-18 23:00:00', '2025-10-18 23:00:00', 30, 300, 270, 60, '마감 근무', 9860, 44370, 4930, 0, 49300, 47670, NULL, NULL),

-- 5. 이알바 @ 올리브영: 9-15시 (야간 0분)
(@wk_lee_olive, '2025-10-19', '2025-10-19 09:00:00', '2025-10-19 09:02:00', '2025-10-19 15:00:00', '2025-10-19 15:00:00', 30, 360, 330, 0, '오전 파트타임', 12000, 66000, 0, 0, 66000, 63820, NULL, NULL),

-- 6. 최알바 @ GS25: (1번과 동일) (야간 0분)
(@wk_choi_gs25, '2025-10-22', '2025-10-22 09:00:00', '2025-10-22 09:00:00', '2025-10-22 18:00:00', '2025-10-22 18:00:00', 60, 540, 480, 0, '반복 근무 테스트', 10000, 80000, 0, 0, 80000, 77360, 'MONDAY', '2025-11-22 00:00:00'),

-- 7. 이알바 @ 홍콩반점: (2번과 동일) (야간 0분)
(@wk_lee_hongkong, '2025-10-23', '2025-10-23 14:00:00', '2025-10-23 14:00:00', '2025-10-23 22:00:00', '2025-10-23 22:00:00', 60, 480, 420, 0, NULL, 11000, 77000, 0, 0, 77000, 74490, 'TUESDAY', '2025-11-23 00:00:00');

SET @work_1 = 1;
SET @work_2 = 2;
SET @work_3 = 3;
SET @work_4 = 4;
SET @work_5 = 5;
SET @work_6 = 6;
SET @work_7 = 7;

-- =====================================
-- 8) WORK_ROUTINE_MAPPINGS (근무-루틴 연결)
-- =====================================
INSERT INTO work_routine_mappings (work_id, routine_id) VALUES
                                                            (@work_1, @r_choi_open),
                                                            (@work_4, @r_choi_open);

-- =====================================
-- 9) ALARMS (알림)
-- =====================================

INSERT INTO normal_alarms (sender_id, receiver_id, title, content, sent_at, read_at) VALUES
                                                                                         (@u_owner_park, @u_choi_worker, '근무 시간 변경 요청', '내일 1시간 일찍 출근 가능할까요?', '2025-09-14 10:00:00', '2025-09-14 10:05:00'),
                                                                                         (@u_owner_park, @u_lee_worker, '급여 지급 완료', '9월 급여가 지급되었습니다. 확인해주세요.', '2025-09-25 11:00:00', NULL),
                                                                                         (@u_choi_worker, @u_owner_park, '업무 관련 문의', '신제품 재고가 부족합니다.', '2025-09-16 14:30:00', '2025-09-16 14:32:00'),
                                                                                         (@u_owner_park, @u_kang_worker, 'GS25 역삼점 근무 초대', '안녕하세요 강알바님, GS25 역삼점에서 함께 일하고 싶습니다. 수락하시겠습니까?', '2025-10-01 09:00:00', NULL),
                                                                                         (@u_kang_worker, @u_owner_park, '근무 초대를 수락했습니다.', '강알바님이 GS25 역삼점 근무 초대를 수락했습니다.', '2025-10-01 11:20:00', '2025-10-01 11:21:00');


INSERT INTO admin_alarms (title, content, sent_at) VALUES
                                                       ('시스템 정기 점검 안내', '서비스 개선을 위해 10월 20일 오전 2시부터 4시까지 시스템 정기 점검이 진행될 예정입니다. 이용에 참고 부탁드립니다.', '2025-10-15 09:00:00'),
                                                       ('추석 연휴 고객센터 운영 안내', '풍성한 한가위 되세요! 추석 연휴 기간 동안 고객센터는 단축 운영됩니다. 자세한 내용은 공지사항을 확인해주세요.', '2025-09-20 10:00:00');

SET @admin_alarm_1 = 1;
SET @admin_alarm_2 = 2;

INSERT INTO admin_alarm_user_mappings (alarm_id, user_id, read_at, deleted_at) VALUES
-- Alarm 1 (System Check) -> 5명 모두에게 발송
(@admin_alarm_1, @u_admin, '2025-10-15 10:00:00', NULL),
(@admin_alarm_1, @u_owner_park, '2025-10-15 11:00:00', NULL),
(@admin_alarm_1, @u_choi_worker, NULL, NULL), -- 아직 안 읽음
(@admin_alarm_1, @u_lee_worker, '2025-10-16 09:00:00', NULL),
(@admin_alarm_1, @u_kang_worker, NULL, NULL), -- 아직 안 읽음
-- Alarm 2 (Chuseok) -> 5명 모두에게 발송
(@admin_alarm_2, @u_admin, '2025-09-20 10:30:00', NULL),
(@admin_alarm_2, @u_owner_park, '2025-09-20 11:00:00', NULL),
(@admin_alarm_2, @u_choi_worker, '2025-09-21 08:00:00', NULL),
(@admin_alarm_2, @u_lee_worker, '2025-09-21 09:00:00', NULL),
(@admin_alarm_2, @u_kang_worker, '2025-09-20 15:00:00', NULL);