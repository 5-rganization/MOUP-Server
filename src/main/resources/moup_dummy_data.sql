-- MOuP 더미데이터 (FK 일관성 보장 버전)
USE moup;

-- 깨끗하게 초기화 (선택)
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE work_routine_mappings;
TRUNCATE TABLE works;
TRUNCATE TABLE routine_tasks;
TRUNCATE TABLE routines;
TRUNCATE TABLE salaries;
TRUNCATE TABLE workers;
TRUNCATE TABLE workplaces;
TRUNCATE TABLE normal_alarms;
TRUNCATE TABLE admin_alarms;
TRUNCATE TABLE social_tokens;
TRUNCATE TABLE user_tokens;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- =====================================
-- 1) USERS
-- =====================================
INSERT INTO users (provider, provider_id, username, nickname, role, profile_img, is_deleted, fcm_token)
VALUES ('LOGIN_GOOGLE','ggl-1001','alice','앨리스','ROLE_OWNER',NULL,0,'fcm_token_alice');
SET @u_alice = LAST_INSERT_ID();

INSERT INTO users (provider, provider_id, username, nickname, role, profile_img, is_deleted, fcm_token)
VALUES ('LOGIN_GOOGLE','ggl-2001','bob','밥','ROLE_WORKER',NULL,0,NULL);
SET @u_bob = LAST_INSERT_ID();

INSERT INTO users (provider, provider_id, username, nickname, role, profile_img, is_deleted, fcm_token)
VALUES ('LOGIN_APPLE','apl-1002','charlie','찰리','ROLE_OWNER',NULL,0,NULL);
SET @u_charlie = LAST_INSERT_ID();

INSERT INTO users (provider, provider_id, username, nickname, role, profile_img, is_deleted, fcm_token)
VALUES ('LOGIN_GOOGLE','ggl-2002','dana','다나','ROLE_WORKER',NULL,0,NULL);
SET @u_dana = LAST_INSERT_ID();

INSERT INTO users (provider, provider_id, username, nickname, role, profile_img, is_deleted, fcm_token)
VALUES ('LOGIN_GOOGLE','ggl-2003','evan','에반','ROLE_WORKER',NULL,0,NULL);
SET @u_evan = LAST_INSERT_ID();

INSERT INTO users (provider, provider_id, username, nickname, role, profile_img, is_deleted, fcm_token)
VALUES ('LOGIN_GOOGLE','ggl-1003','fiona','피오나','ROLE_OWNER',NULL,0,NULL);
SET @u_fiona = LAST_INSERT_ID();

INSERT INTO users (provider, provider_id, username, nickname, role, profile_img, is_deleted, fcm_token)
VALUES ('LOGIN_APPLE','apl-2004','grace','그레이스','ROLE_WORKER',NULL,0,NULL);
SET @u_grace = LAST_INSERT_ID();

INSERT INTO users (provider, provider_id, username, nickname, role, profile_img, is_deleted, fcm_token)
VALUES ('LOGIN_GOOGLE','ggl-2005','henry','헨리','ROLE_WORKER',NULL,0,NULL);
SET @u_henry = LAST_INSERT_ID();

-- 토큰
INSERT INTO social_tokens (user_id, refresh_token, updated_at) VALUES
                                                                   (@u_alice,'refresh_alice_XXX',NOW()),
                                                                   (@u_charlie,'refresh_charlie_YYY',NOW()),
                                                                   (@u_grace,'refresh_grace_ZZZ',NOW());

INSERT INTO user_tokens (user_id, refresh_token, expiry_date, created_at) VALUES
                                                                              (@u_bob,'rt_bob_111',DATE_ADD(NOW(), INTERVAL 30 DAY),NOW()),
                                                                              (@u_dana,'rt_dana_222',DATE_ADD(NOW(), INTERVAL 45 DAY),NOW()),
                                                                              (@u_grace,'rt_grace_333',DATE_ADD(NOW(), INTERVAL 60 DAY),NOW());

-- =====================================
-- 2) WORKPLACES
-- =====================================
INSERT INTO workplaces (owner_id, workplace_name, category_name, is_shared, address, latitude, longitude)
VALUES (@u_alice,'Cafe MoUp','CAFE',0,'서울시 중구 어딘가 1',37.566536,126.977966);
SET @wp_cafe = LAST_INSERT_ID();

INSERT INTO workplaces (owner_id, workplace_name, category_name, is_shared, address, latitude, longitude)
VALUES (@u_charlie,'OneStop Mart','MART',0,'서울시 강남구 어딘가 2',37.497942,127.027621);
SET @wp_mart = LAST_INSERT_ID();

INSERT INTO workplaces (owner_id, workplace_name, category_name, is_shared, address, latitude, longitude)
VALUES (NULL,'Popup Store','POPUP',1,'서울시 마포구 어딘가 3',37.549889,126.914561);
SET @wp_popup = LAST_INSERT_ID();

INSERT INTO workplaces (owner_id, workplace_name, category_name, is_shared, address, latitude, longitude)
VALUES (@u_fiona,'NightOwl PC','PC_BANG',0,'서울시 송파구 어딘가 4',37.511197,127.098129);
SET @wp_pc = LAST_INSERT_ID();

-- =====================================
-- 3) WORKERS (user↔workplace)
-- =====================================
INSERT INTO workers (user_id, workplace_id, worker_based_label_color, owner_based_label_color, is_accepted)
VALUES (@u_bob, @wp_cafe, '#FFAA00', '#0088FF', 1);
SET @wk_bob = LAST_INSERT_ID();

INSERT INTO workers (user_id, workplace_id, worker_based_label_color, owner_based_label_color, is_accepted)
VALUES (@u_dana, @wp_mart, '#55CC77', '#CC5577', 1);
SET @wk_dana = LAST_INSERT_ID();

INSERT INTO workers (user_id, workplace_id, worker_based_label_color, owner_based_label_color, is_accepted)
VALUES (@u_evan, @wp_popup, '#999999', NULL, 0);
SET @wk_evan = LAST_INSERT_ID();

INSERT INTO workers (user_id, workplace_id, worker_based_label_color, owner_based_label_color, is_accepted)
VALUES (@u_grace, @wp_pc, '#AA66FF', '#66FFAA', 1);
SET @wk_grace = LAST_INSERT_ID();

INSERT INTO workers (user_id, workplace_id, worker_based_label_color, owner_based_label_color, is_accepted)
VALUES (@u_henry, @wp_pc, '#4444FF', '#FF4444', 1);
SET @wk_henry = LAST_INSERT_ID();

-- =====================================
-- 4) ROUTINES (per user) + TASKS
-- =====================================
-- Bob
INSERT INTO routines (user_id, routine_name, alarm_time) VALUES
    (@u_bob,'오픈체크','08:50:00');
SET @r_bob_open = LAST_INSERT_ID();

INSERT INTO routine_tasks (routine_id, content, order_index, is_checked) VALUES
                                                                             (@r_bob_open,'키오스크 전원',1,0),
                                                                             (@r_bob_open,'원두/소모품 보충',2,0),
                                                                             (@r_bob_open,'POS 점검',3,0);

-- Dana
INSERT INTO routines (user_id, routine_name, alarm_time) VALUES
    (@u_dana,'마감체크','22:05:00');
SET @r_dana_close = LAST_INSERT_ID();

INSERT INTO routine_tasks (routine_id, content, order_index, is_checked) VALUES
                                                                             (@r_dana_close,'유통기한 확인',1,0),
                                                                             (@r_dana_close,'폐기 등록',2,0),
                                                                             (@r_dana_close,'캐셔 정산',3,0);

-- Evan
INSERT INTO routines (user_id, routine_name, alarm_time) VALUES
    (@u_evan,'알바준비','10:00:00');
SET @r_evan_prep = LAST_INSERT_ID();

INSERT INTO routine_tasks (routine_id, content, order_index, is_checked) VALUES
                                                                             (@r_evan_prep,'유니폼/명찰',1,0),
                                                                             (@r_evan_prep,'출근 보고',2,0);

-- Grace
INSERT INTO routines (user_id, routine_name, alarm_time) VALUES
    (@u_grace,'오픈체크','11:50:00');
SET @r_grace_open = LAST_INSERT_ID();

INSERT INTO routine_tasks (routine_id, content, order_index, is_checked) VALUES
                                                                             (@r_grace_open,'PC 부팅/패치',1,0),
                                                                             (@r_grace_open,'음료 냉장고 점검',2,0);

-- Henry (루틴 추가해 매핑 가능하게)
INSERT INTO routines (user_id, routine_name, alarm_time) VALUES
    (@u_henry,'저녁점검','17:45:00');
SET @r_henry_evening = LAST_INSERT_ID();

INSERT INTO routine_tasks (routine_id, content, order_index, is_checked) VALUES
    (@r_henry_evening,'헤드셋/마이크 점검',1,0);

-- =====================================
-- 5) WORKS (근무 스케줄/실근무)
-- =====================================
-- Bob @ Cafe (2건)
INSERT INTO works (worker_id, work_date, start_time, actual_start_time, end_time, actual_end_time, rest_time, memo, daily_income, hourly_rate, repeat_days, repeat_end_date)
VALUES (@wk_bob,'2025-10-10','09:00:00','09:05:00','13:00:00','13:10:00','00:15:00','오픈 대체',48000,12000,'MON,WED,FRI','2025-12-31 00:00:00');
SET @work_bob_1 = LAST_INSERT_ID();

INSERT INTO works (worker_id, work_date, start_time, actual_start_time, end_time, actual_end_time, rest_time, memo, daily_income, hourly_rate, repeat_days, repeat_end_date)
VALUES (@wk_bob,'2025-10-12','09:00:00','09:02:00','13:00:00','13:05:00','00:10:00','브런치 러시',48000,12000,'MON,WED,FRI','2025-12-31 00:00:00');
SET @work_bob_2 = LAST_INSERT_ID();

-- Dana @ Mart (1건)
INSERT INTO works (worker_id, work_date, start_time, actual_start_time, end_time, actual_end_time, rest_time, memo, daily_income, hourly_rate, repeat_days, repeat_end_date)
VALUES (@wk_dana,'2025-10-11','14:00:00','13:55:00','22:00:00','22:05:00','01:00:00','마감 교육',NULL,NULL,'TUE,THU,SAT','2025-12-31 00:00:00');
SET @work_dana_1 = LAST_INSERT_ID();

-- Grace @ PC방 (1건)
INSERT INTO works (worker_id, work_date, start_time, actual_start_time, end_time, actual_end_time, rest_time, memo, daily_income, hourly_rate, repeat_days, repeat_end_date)
VALUES (@wk_grace,'2025-10-11','12:00:00','11:58:00','18:00:00','18:10:00','00:30:00','오픈/음료 진열',90000,NULL,'SAT,SUN','2025-11-30 00:00:00');
SET @work_grace_1 = LAST_INSERT_ID();

-- Henry @ PC방 (1건)
INSERT INTO works (worker_id, work_date, start_time, actual_start_time, end_time, actual_end_time, rest_time, memo, daily_income, hourly_rate, repeat_days, repeat_end_date)
VALUES (@wk_henry,'2025-10-11','18:00:00','18:01:00','23:00:00','23:02:00','00:20:00','피크타임 지원',NULL,11000,'FRI,SAT','2025-12-31 00:00:00');
SET @work_henry_1 = LAST_INSERT_ID();

-- =====================================
-- 6) WORK_ROUTINE_MAPPINGS (works.id ↔ routines.id)
-- =====================================
INSERT INTO work_routine_mappings (work_id, routine_id) VALUES
                                                            (@work_bob_1,   @r_bob_open),
                                                            (@work_bob_2,   @r_bob_open),
                                                            (@work_dana_1,  @r_dana_close),
                                                            (@work_grace_1, @r_grace_open),
                                                            (@work_henry_1, @r_henry_evening);

-- =====================================
-- 7) SALARIES (급여 정책)
-- =====================================
-- Bob: 주급/시급
INSERT INTO salaries (worker_id, salary_type, salary_calculation, hourly_rate, fixed_rate, salary_date, salary_day,
                      has_national_pension, has_health_insurance, has_employment_insurance, has_industrial_accident, has_income_tax, has_night_allowance)
VALUES (@wk_bob,'SALARY_WEEKLY','SALARY_CALCULATION_HOURLY',12000,NULL,NULL,'FRIDAY',0,0,1,1,1,0);

-- Dana: 월급/고정, 25일
INSERT INTO salaries (worker_id, salary_type, salary_calculation, hourly_rate, fixed_rate, salary_date, salary_day,
                      has_national_pension, has_health_insurance, has_employment_insurance, has_industrial_accident, has_income_tax, has_night_allowance)
VALUES (@wk_dana,'SALARY_MONTHLY','SALARY_CALCULATION_FIXED',NULL,2000000,25,NULL,1,1,1,1,1,1);

-- Evan: 주급/시급(대기지만 정책 선등록)
INSERT INTO salaries (worker_id, salary_type, salary_calculation, hourly_rate, fixed_rate, salary_date, salary_day,
                      has_national_pension, has_health_insurance, has_employment_insurance, has_industrial_accident, has_income_tax, has_night_allowance)
VALUES (@wk_evan,'SALARY_WEEKLY','SALARY_CALCULATION_HOURLY',13000,NULL,NULL,'SATURDAY',0,0,1,1,1,0);

-- Grace: 일급/고정
INSERT INTO salaries (worker_id, salary_type, salary_calculation, hourly_rate, fixed_rate, salary_date, salary_day,
                      has_national_pension, has_health_insurance, has_employment_insurance, has_industrial_accident, has_income_tax, has_night_allowance)
VALUES (@wk_grace,'SALARY_DAILY','SALARY_CALCULATION_FIXED',NULL,100000,NULL,NULL,0,0,1,1,1,0);

-- Henry: 주급/시급
INSERT INTO salaries (worker_id, salary_type, salary_calculation, hourly_rate, fixed_rate, salary_date, salary_day,
                      has_national_pension, has_health_insurance, has_employment_insurance, has_industrial_accident, has_income_tax, has_night_allowance)
VALUES (@wk_henry,'SALARY_WEEKLY','SALARY_CALCULATION_HOURLY',11000,NULL,NULL,'FRIDAY',0,0,1,1,1,1);

-- =====================================
-- 8) ALARMS
-- =====================================
INSERT INTO normal_alarms (sender_id, receiver_id, title, content, sent_at, read_at, alarm_type) VALUES
                                                                                                     (@u_alice, @u_evan,  '근무지 초대', 'Popup Store로 초대합니다.', '2025-10-08 10:00:00', NULL, 'ALARM_INVITE_REQUEST'),
                                                                                                     (@u_evan,  @u_alice, '초대 거절', '개인 사정으로 이번 주는 어렵습니다.', '2025-10-08 12:00:00', '2025-10-08 12:10:00', 'ALARM_INVITE_REJECT'),
                                                                                                     (@u_fiona, @u_grace, '공지 알림', '주말 오픈 10분 전 점검 부탁', '2025-10-10 11:30:00', '2025-10-10 11:35:00', 'ALARM_NOTIFICATION'),
                                                                                                     (@u_alice, @u_bob,   '초대 수락', 'Cafe MoUp 근무 배정 완료', '2025-10-09 09:00:00', '2025-10-09 09:05:00', 'ALARM_INVITE_ACCEPT');

INSERT INTO admin_alarms (title, content, sent_at, alarm_type) VALUES
    ('시스템 점검 안내','10/15 02:00~03:00 서버 점검 예정','2025-10-12 09:00:00','ALARM_ANNOUNCEMENT');
