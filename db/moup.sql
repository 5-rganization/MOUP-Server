DROP SCHEMA IF EXISTS moup;
CREATE SCHEMA moup;
USE moup;

CREATE TABLE `users`
(
    `id`          BIGINT AUTO_INCREMENT                                              NOT NULL PRIMARY KEY,
    `provider`    ENUM ('LOGIN_GOOGLE', 'LOGIN_APPLE', 'LOGIN_NAVER', 'LOGIN_KAKAO') NOT NULL,
    `provider_id` VARCHAR(100)                                                       NOT NULL,
    `username`    VARCHAR(20),
    `nickname`    VARCHAR(20),
    `role`        ENUM ('ROLE_WORKER', 'ROLE_OWNER', 'ROLE_ADMIN')                   DEFAULT 'ROLE_WORKER',
    `profile_img` VARCHAR(255),
    `created_at`  TIMESTAMP                                                          DEFAULT CURRENT_TIMESTAMP(),
    `deleted_at`  TIMESTAMP,
    `is_deleted`  TINYINT(1)                                                         DEFAULT 0,
    -- 탈퇴 확정 처리(가명처리)가 끝난 시각.
    -- 하드 삭제를 없앴으므로 `is_deleted = 1` 행이 영구히 남는다. 이 컬럼이 없으면
    -- 배치가 이미 처리한 사용자를 매일 다시 집어 소셜 연동 해제를 무한 재시도한다.
    -- `is_deleted = 1 AND anonymized_at IS NULL` = "탈퇴 신청됨, 아직 처리 안 됨".
    `anonymized_at` TIMESTAMP                                                        NULL,
    UNIQUE KEY `unique_provider` (`provider`, `provider_id`)
);

-- 토큰 DB --
-- FCM 등록 토큰은 users의 단일 컬럼이었다. 그래서 세 가지 문제가 있었다:
--   1) 같은 기기에서 계정을 바꿔 로그인하면 두 유저 행이 동일한 토큰을 갖고,
--      먼저 쓰던 사람의 알림이 지금 쓰는 사람 폰에 배달됐다 (실명 포함).
--   2) 기기를 여러 대 쓰면 마지막 로그인 기기만 푸시를 받았다.
--   3) 로그아웃이 컬럼을 통째로 비워, 폰에서 로그아웃하면 태블릿 푸시도 죽었다.
-- 토큰 자체가 앱 설치 단위 식별자이므로 UNIQUE (token)이 1)을 구조적으로 막는다.
CREATE TABLE `fcm_tokens`
(
    `id`         BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `user_id`    BIGINT                NOT NULL,
    `token`      VARCHAR(512)          NOT NULL,
    `updated_at` DATETIME              NOT NULL DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP(),
    UNIQUE KEY `uk_fcm_tokens_token` (`token`),
    INDEX `idx_fcm_tokens_user` (`user_id`),
    FOREIGN KEY (`user_id`) REFERENCES users (`id`) ON DELETE CASCADE
);

CREATE TABLE `social_tokens`
(
    `id`            BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `user_id`       BIGINT                NOT NULL,
    `refresh_token` TEXT                  NULL,
    `updated_at`    TIMESTAMP,
    UNIQUE KEY `uk_social_tokens_user` (`user_id`),
    FOREIGN KEY (`user_id`) REFERENCES users (`id`) ON DELETE CASCADE
);

CREATE TABLE `user_tokens`
(
    `id`            BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `user_id`       BIGINT                NOT NULL,
    `refresh_token` TEXT                  NOT NULL,
    `expiry_date`   DATETIME              NULL,
    `created_at`    DATETIME              NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    UNIQUE KEY `uk_user_tokens_user` (`user_id`),
    FOREIGN KEY (`user_id`) REFERENCES users (`id`) ON DELETE CASCADE
);

-- 루틴 DB --
CREATE TABLE `routines`
(
    `id`           BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `user_id`      BIGINT                NOT NULL,
    `routine_name` VARCHAR(20)           NOT NULL,
    `alarm_time`   TIME                  NULL,
    UNIQUE KEY `uk_routines_user_name` (`user_id`, `routine_name`),
    FOREIGN KEY (`user_id`) REFERENCES users (`id`) ON DELETE CASCADE
);

CREATE TABLE `routine_tasks`
(
    `id`          BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `routine_id`  BIGINT                NOT NULL,
    `content`     VARCHAR(30)           NOT NULL,
    `order_index` INT                   NOT NULL,
    FOREIGN KEY (`routine_id`) REFERENCES routines (`id`) ON DELETE CASCADE,
    UNIQUE KEY `unique_routine_order` (`routine_id`, `order_index`)
);

CREATE TABLE `normal_alarms`
(
    `id`          BIGINT AUTO_INCREMENT                                                                             NOT NULL PRIMARY KEY,
    `sender_id`   BIGINT                                                                                            NULL,
    `receiver_id` BIGINT                                                                                            NOT NULL,
    `title`       TEXT                                                                                              NOT NULL,
    `content`     TEXT                                                                                              NULL,
    `sent_at`     DATETIME                                                                                          NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    `read_at`     DATETIME                                                                                          NULL,
    -- 모든 조회가 receiver_id 기준이다. 인덱스가 없어 매번 풀스캔이었다.
    INDEX `idx_normal_alarms_receiver` (`receiver_id`, `sent_at`),
    -- FK가 없어 유저를 하드 삭제해도 알림이 고아 행으로 영구 잔존했다.
    -- 수신자가 사라지면 그 사람의 알림함도 사라진다. 발신자만 사라진 경우는 알림을 남긴다.
    FOREIGN KEY (`receiver_id`) REFERENCES users (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`sender_id`) REFERENCES users (`id`) ON DELETE SET NULL
);

CREATE TABLE `admin_alarms`
(
    `id`         BIGINT AUTO_INCREMENT       NOT NULL PRIMARY KEY,
    `title`      TEXT                        NOT NULL,
    `content`    TEXT                        NULL,
    `sent_at`    DATETIME                    NOT NULL DEFAULT CURRENT_TIMESTAMP()
);

CREATE TABLE `admin_alarm_user_mappings` (
    `id` BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `alarm_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `read_at` DATETIME NULL,
    `deleted_at` DATETIME NULL,
    UNIQUE KEY `uk_admin_alarm_user` (`alarm_id`, `user_id`),
    FOREIGN KEY (`alarm_id`) REFERENCES admin_alarms(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES users(`id`) ON DELETE CASCADE
);
--

-- 근무지 DB --
CREATE TABLE `workplaces`
(
    `id`             BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `owner_id`       BIGINT                NULL,
    `workplace_name` VARCHAR(50)           NOT NULL,
    `category_name`  VARCHAR(10)           NOT NULL,
    `is_shared`      TINYINT(1) DEFAULT 0  NOT NULL,
    `address`        VARCHAR(100)          NULL,
    `latitude`       DECIMAL(9, 6)         NULL,
    `longitude`      DECIMAL(9, 6)         NULL,
    UNIQUE KEY `uk_workplaces_owner_name` (`owner_id`, `workplace_name`),
    CONSTRAINT `fk_workplaces_owner` FOREIGN KEY (`owner_id`) REFERENCES users (`id`) ON DELETE SET NULL
);

CREATE TABLE `workers`
(
    `id`                       BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `user_id`                  BIGINT                NULL,
    `workplace_id`             BIGINT                NOT NULL,
    `worker_based_label_color` VARCHAR(10)           NULL,
    `owner_based_label_color`  VARCHAR(10)           NULL,
    -- 승인 게이트의 전제. NULL을 허용하면 "미검사"와 "미승인"이 구분되지 않는다.
    `is_accepted`              TINYINT(1)            NOT NULL DEFAULT 0,
    `is_now_working`           TINYINT(1)            NULL,
    -- 참여는 check-then-insert였다. 동시 요청 두 건이면 같은 근무지에 행이 2개 생기고
    -- findByUserIdAndWorkplaceId가 TooManyResultsException으로 터져 해당 근무지가 영구 500이 된다.
    UNIQUE KEY `uk_workers_workplace_user` (`workplace_id`, `user_id`),
    FOREIGN KEY (`user_id`) REFERENCES users (`id`) ON DELETE SET NULL,
    FOREIGN KEY (`workplace_id`) REFERENCES workplaces (`id`) ON DELETE CASCADE
);

CREATE TABLE `works`
(
    `id`                      BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `worker_id`               BIGINT                NOT NULL,
    `work_date`               DATE                  NOT NULL,
    `start_time`              DATETIME              NOT NULL,
    `actual_start_time`       DATETIME              NULL,
    `end_time`                DATETIME              NULL,
    `actual_end_time`         DATETIME              NULL,
    `rest_time_minutes`       INT                   DEFAULT 0,
    `gross_work_minutes`      INT                   DEFAULT 0,  -- 총 근무시간(분)
    `net_work_minutes`        INT                   DEFAULT 0,  -- 순 근무시간(분, 휴게시간 제외)
    `night_work_minutes`      INT                   DEFAULT 0,  -- 야간 근무시간(분, 휴게시간 제외)
    `memo`                    VARCHAR(200)          NULL,
    `hourly_rate`             INT                   NULL,
    -- 수당 적용 여부 스냅샷. hourly_rate와 달리 예전에는 salaries의 **현재값**을 읽어,
    -- 나중에 수당을 켜면 그 주의 근무 하나만 수정해도 그 주 전체가 새 정책으로 재계산됐다.
    -- salaries는 제자리 UPDATE라 이력이 없어 옛 설정을 복원할 방법도 없었다.
    -- 확정 정책 3(급여 스냅샷) 위반.
    `has_night_allowance`     TINYINT(1)            NOT NULL DEFAULT 0,
    `has_holiday_allowance`   TINYINT(1)            NOT NULL DEFAULT 0,
    `base_pay`                INT                   DEFAULT 0,  -- 기본급 (휴게시간 제외)
    `night_allowance`         INT                   DEFAULT 0,  -- 야간수당
    `holiday_allowance`       INT                   DEFAULT 0,  -- 주휴수당 (해당 주에 발생한 수당을 N등분하여 일별로 저장)
    `gross_income`            INT                   DEFAULT 0,  -- 세전 일급 (위 4가지의 합)
    `estimated_net_income`    INT                   DEFAULT 0,  -- 추정 세후 일급 (캘린더 표시용)
    `repeat_group_id`         VARCHAR(36)           NULL,
    FOREIGN KEY (`worker_id`) REFERENCES workers (`id`) ON DELETE CASCADE,
    INDEX `idx_repeat_group_id` (`repeat_group_id`),
    -- 조회는 거의 전부 worker_id + work_date 범위다. FK가 만드는 단일 인덱스로는 filesort가 남는다.
    INDEX `idx_works_worker_date` (`worker_id`, `work_date`)
);

CREATE TABLE `work_routine_mappings`
(
    `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
    `work_id`    BIGINT NOT NULL,
    `routine_id` BIGINT NOT NULL,
    FOREIGN KEY (`work_id`) REFERENCES works (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`routine_id`) REFERENCES routines (`id`) ON DELETE CASCADE
);

-- 근무별 할 일 완료(체크) 상태.
--
-- 예전에는 `is_done`/`completed`/`checked` 계열 컬럼이 스키마 전체에 0건이었다.
-- "오늘 이 근무의 이 할 일을 완료했다"를 서버가 보관하지 않아 기기를 바꾸면 사라졌다.
--
-- 같은 루틴이 여러 근무에 연결되므로 완료는 반드시 (근무, 할 일) 쌍이다.
-- routine_task_id 하나로는 "어느 날 근무에서 했는지"를 구분할 수 없다.
--
-- UNIQUE가 토글의 멱등성을 보장한다. 더블탭이나 재시도로 중복 행이 생기지 않는다.
CREATE TABLE `routine_task_completions`
(
    `id`              BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    `work_id`         BIGINT                NOT NULL,
    `routine_task_id` BIGINT                NOT NULL,
    `completed_at`    DATETIME              NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    UNIQUE KEY `uk_routine_task_completion` (`work_id`, `routine_task_id`),
    FOREIGN KEY (`work_id`) REFERENCES works (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`routine_task_id`) REFERENCES routine_tasks (`id`) ON DELETE CASCADE
);

CREATE TABLE `salaries`
(
    `id`                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    `worker_id`                BIGINT                                                                              NOT NULL,
    `salary_type`              ENUM ('SALARY_MONTHLY', 'SALARY_WEEKLY', 'SALARY_DAILY')                            NOT NULL,
    `salary_calculation`       ENUM ('SALARY_CALCULATION_HOURLY', 'SALARY_CALCULATION_FIXED')                      NOT NULL,
    `hourly_rate`              INT                                                                                 NULL,
    `fixed_rate`               INT                                                                                 NULL,
    `salary_date`              INT CHECK (salary_date >= 1 AND salary_date <= 31)                                  NULL,
    `salary_day`               ENUM ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY') NULL,
    `has_national_pension`     TINYINT(1)                                                                          NOT NULL,
    `has_health_insurance`     TINYINT(1)                                                                          NOT NULL,
    `has_employment_insurance` TINYINT(1)                                                                          NOT NULL,
    `has_industrial_accident`  TINYINT(1)                                                                          NOT NULL,
    `has_income_tax`           TINYINT(1)                                                                          NOT NULL,
    `has_holiday_allowance`    TINYINT(1)                                                                          NOT NULL,
    `has_night_allowance`      TINYINT(1)                                                                          NOT NULL,
    -- 시급제인데 hourly_rate가 NULL이면 급여 전체가 0원이 되거나 언박싱 NPE로 500이 난다.
    CONSTRAINT `ck_salaries_rate` CHECK (
        (`salary_calculation` = 'SALARY_CALCULATION_HOURLY' AND `hourly_rate` IS NOT NULL)
     OR (`salary_calculation` = 'SALARY_CALCULATION_FIXED'  AND `fixed_rate`  IS NOT NULL)
    ),
    FOREIGN KEY (`worker_id`) REFERENCES workers (`id`) ON DELETE CASCADE
);
--
