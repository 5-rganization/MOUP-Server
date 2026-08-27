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
    `fcm_token`   TEXT,
    UNIQUE KEY `unique_provider` (`provider`, `provider_id`)
);

-- 토큰 DB --
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
